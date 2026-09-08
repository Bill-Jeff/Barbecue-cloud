package com.bbq.module.crm.chatbot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry;
import com.bbq.module.crm.chatbot.model.ChatbotFlowConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 微信客服「拉取模式」消息轮询器——个人开发者免回调方案：
 *
 * 企微后台回调 URL 有「备案域名主体一致」硬校验，个人开发者过不了。
 * 微信客服另支持不配置回调、定时调用 sync_msg 接口主动拉取消息：
 *   POST https://qyapi.weixin.qq.com/cgi-bin/kf/sync_msg?access_token=xxx
 *   body: { cursor: "", token: "", limit: 1000 }  返回 next_cursor + msg_list
 *
 * 游标存 Redis，服务重启也能续着拉；msgid 落 Redis Set 去重防重复处理。
 * 只处理 origin=3（客户发送）的 text 消息，喂给 ChatbotOrchestrator，
 * 「客户进入会话即开启追问」的场景同样被覆盖（进入事件之后客户的第一条消息会触发开场提问）。
 */
@Slf4j
@Service
public class WechatKfMessagePuller {

    private static final String CURSOR_KEY = "chatbot:wecom:kf:sync_cursor";
    private static final String PROCESSED_KEY = "chatbot:wecom:kf:processed_msgids";
    private static final String SYNC_MSG_URL = "https://qyapi.weixin.qq.com/cgi-bin/kf/sync_msg?access_token=";

    /** 回调事件里的 Token（10 分钟有效），传给 sync_msg 可解除「无 token 严格频率限制」 */
    public static final String CB_TOKEN_KEY = "chatbot:wecom:kf:callback_token";

    /** origin=3 表示客户在微信客服会话中发送的消息 */
    private static final int ORIGIN_CUSTOMER = 3;

    @Resource
    private ChatbotFlowRegistry flowRegistry;

    @Resource
    private WeComTokenService tokenService;

    @Resource
    private ChatbotOrchestrator orchestrator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile boolean configWarned = false;

    /**
     * 退避档位表（毫秒）。下标 = 连续错误次数（0 = 无退避）。
     * 1次错误->30s；2次->60s；3次->120s；4次及以上封顶到300s（5 分钟）。
     * 设计目标：在 95000 / 45009 这种"短时间内不会自愈"的场景里，
     * 不要再每 5s 把 sync_msg 打爆，避免被企微在 IP 维度临时封禁。
     */
    private static final long[] BACKOFF_STEPS_MS = {0L, 30_000L, 60_000L, 120_000L, 300_000L};

    /** 连续错误次数。成功 / token 失效清零时重置 */
    private final AtomicLong consecutiveErrors = new AtomicLong(0);

    /** 退避期截止时间戳（毫秒）。当前时间 < 该值则跳过本次 sync_msg */
    private final AtomicLong backoffUntilMs = new AtomicLong(0);

    /** 诊断状态（供 ChatbotStatusController 读取）：最近一次拉取尝试/成功时间与最近 errcode */
    private volatile long lastAttemptAtMs = 0L;
    private volatile long lastSuccessAtMs = 0L;
    private volatile String lastErrcode = "-";

    /** 拉取互斥锁：定时轮询与回调触发共用，保证同一时刻只有一个 sync_msg 在途 */
    private final AtomicBoolean pulling = new AtomicBoolean(false);

    /** 回调触发的拉取跑独立单线程，不占用回调 HTTP 线程、不与 @Scheduled 线程争抢 */
    private final ExecutorService triggerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chatbot-kf-pull-trigger");
        t.setDaemon(true);
        return t;
    });

    /**
     * 拉取间隔默认 30s，可通过属性 chatbot.pullIntervalMs 覆盖（如 -Dchatbot.pullIntervalMs=10000）。
     * 不能用 5s 级高频：纯拉取模式下 sync_msg 的 token 参数传空（无回调来源），
     * 官方文档明确「不填 token 接口有严格的频率限制」，社区实测 5s 一次就会 45009。
     */
    @Scheduled(fixedDelayString = "${chatbot.pullIntervalMs:30000}")
    public void pull() {
        var kf = flowRegistry.getConfig() != null ? flowRegistry.getConfig().getWechatKf() : null;
        if (kf == null || isBlank(kf.getCorpId()) || isBlank(kf.getSecret())) {
            if (!configWarned) {
                log.info("[chatbot] 未配置 corpId/secret，拉取模式未启用（补配置后无需重启也会自动生效）");
                configWarned = true;
            }
            return;
        }
        // 主动关闭 sync_msg 拉取：callback 模式已经跑通，避免双通道重复处理
        if (!kf.isPullModeEnabled()) {
            return;
        }
        // 退避期未结束：直接跳过，sync_msg 一次都不发，日志也不刷
        long now = System.currentTimeMillis();
        long until = backoffUntilMs.get();
        if (now < until) {
            return;
        }
        runPull(kf);
    }

    /** 定时轮询与回调触发共用的执行体：CAS 互斥，保证同一时刻只有一个 sync_msg 在途 */
    private void runPull(ChatbotFlowConfig.WechatKfConfig kf) {
        if (!pulling.compareAndSet(false, true)) {
            return;
        }
        try {
            lastAttemptAtMs = System.currentTimeMillis();
            pullOnce(kf);
        } catch (Exception e) {
            // 这里兜底防异常逃逸：网络/序列化挂掉也要进退避，否则会刷日志
            lastErrcode = "exception";
            log.error("[chatbot] sync_msg 调用异常，将进入退避", e);
            scheduleBackoff();
        } finally {
            pulling.set(false);
        }
    }

    /**
     * 回调触发入口：收到企微推送立刻拉一次（不等下一个轮询周期），实现秒级响应。
     * 退避期内不触发，避免限流雪上加霜。
     */
    public void triggerFromCallback() {
        if (System.currentTimeMillis() < backoffUntilMs.get()) {
            return;
        }
        triggerExecutor.execute(() -> {
            ChatbotFlowConfig.WechatKfConfig kf =
                    flowRegistry.getConfig() != null ? flowRegistry.getConfig().getWechatKf() : null;
            if (kf == null || !kf.isPullModeEnabled() || isBlank(kf.getCorpId()) || isBlank(kf.getSecret())) {
                return;
            }
            runPull(kf);
        });
    }

    private void pullOnce(ChatbotFlowConfig.WechatKfConfig kf) throws Exception {
        String token = tokenService.getAccessToken();
        if (token == null) {
            return;
        }
        String cursor = stringRedisTemplate.opsForValue().get(CURSOR_KEY);

        JSONObject body = new JSONObject();
        body.put("cursor", cursor == null ? "" : cursor);
        // 回调事件带来的 Token（10 分钟有效）传给 sync_msg 可解除「无 token 严格频率限制」；
        // 没有回调时传空走 30s 兜底轮询（30s 间隔远低于该阈值，不会 45009）
        String cbToken = stringRedisTemplate.opsForValue().get(CB_TOKEN_KEY);
        body.put("token", isBlank(cbToken) ? "" : cbToken);
        body.put("limit", 1000);
        // sync_msg 按客服账号拉取（官方文档 open_kfid 标注必填）：
        // 应用当前没有任何「可调用」授权账号时不带它会报 95000 invalid open_kfid
        if (!isBlank(kf.getOpenKfid())) {
            body.put("open_kfid", kf.getOpenKfid());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SYNC_MSG_URL + token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject result = JSON.parseObject(resp.body());

        int errcode = result.getIntValue("errcode");
        lastErrcode = String.valueOf(errcode);
        if (errcode == 42001 || errcode == 40014) {
            // token 失效：清缓存 + 退避计数清零，下轮自动重取，**不计入错误**（拿新 token 后往往就正常了）
            tokenService.invalidate();
            resetBackoff("token 已失效，将自动重取");
            return;
        }
        if (errcode == 60020) {
            // 配置类问题：IP 白名单没配。日志只刷一次最关键那行（防洪水），进弱退避
            log.error("[chatbot] 出口 IP 不在企微「企业可信IP」白名单里，请到企微后台添加当前出口 IP 后无需重启即可恢复");
            scheduleBackoff();
            return;
        }
        if (errcode == 45009) {
            // 企微明确限流：必须立刻退避，否则会被 IP 维度临时封禁
            scheduleBackoff();
            log.warn("[chatbot] sync_msg 被企微限流（errcode=45009 Too many requests），进入退避，{}s 后再试",
                    currentBackoffSeconds());
            return;
        }
        if (errcode == 95000) {
            // invalid open_kfid：根因几乎一定是 secret 配错了 —— 用的是自建应用 secret / 旧主体 secret，
            // 正确做法是到当前主体（chatbot-flow.yaml 中 wechat_kf.corpId 对应）的「微信客服后台」→「开发配置」
            // 拿 kf 专属 secret。
            scheduleBackoff();
            log.error("[chatbot] sync_msg 返回 95000 invalid open_kfid：当前 secret 与 corpId 不匹配。" +
                            "请到【微信客服后台 → 开发配置】获取该主体的「客服 secret」（不是自建应用 secret、不是其它主体 secret），" +
                            "更新 chatbot-flow.yaml 中 wechat_kf.secret 后无需重启即可生效。退避 {}s 后重试。",
                    currentBackoffSeconds());
            return;
        }
        if (errcode != 0) {
            // 其它业务错误：照样进退避，避免失败循环刷屏
            scheduleBackoff();
            log.error("[chatbot] sync_msg 拉取失败 errcode={} body={}（退避 {}s 后再试）",
                    errcode, resp.body(), currentBackoffSeconds());
            return;
        }

        // 走到这里说明 sync_msg 至少没拒绝：成功（或 msg_list 干脆为空也算成功），重置退避
        resetBackoff(null);
        lastSuccessAtMs = System.currentTimeMillis();

        JSONArray msgList = result.getJSONArray("msg_list");
        if (msgList != null) {
            for (int i = 0; i < msgList.size(); i++) {
                handleMsg(msgList.getJSONObject(i));
            }
        }

        String nextCursor = result.getString("next_cursor");
        if (nextCursor != null && !nextCursor.isBlank()) {
            // 游标持久化：服务重启后接着上次位置拉
            stringRedisTemplate.opsForValue().set(CURSOR_KEY, nextCursor, Duration.ofDays(7));
        }
    }

    /**
     * 错误计数 +1，按当前计数查退避档位，更新 backoffUntilMs。
     * 进退避的"第一次"会打一行 WARN，之后静默（同档位内不重复打）。
     */
    private void scheduleBackoff() {
        long errs = consecutiveErrors.incrementAndGet();
        int idx = (int) Math.min(errs, BACKOFF_STEPS_MS.length - 1);
        long delayMs = BACKOFF_STEPS_MS[idx];
        backoffUntilMs.set(System.currentTimeMillis() + delayMs);
        // 仅在"首次进入退避"或"档位升级"时打一行提示，避免 5s 一刷
        if (errs == 1) {
            log.warn("[chatbot] 进入退避模式：第 1 次错误，下次 sync_msg 至少 {}s 后再试", delayMs / 1000);
        }
    }

    /**
     * 成功 / token 已重置 等场景把退避计数清零。
     * 若之前在退避中、成功后才打一行 INFO，便于排查。
     */
    private void resetBackoff(String reason) {
        long prev = consecutiveErrors.getAndSet(0);
        if (backoffUntilMs.getAndSet(0) > 0 || prev > 0) {
            if (reason != null) {
                log.info("[chatbot] 退出退避模式：{}", reason);
            } else {
                log.info("[chatbot] 退出退避模式：sync_msg 已恢复");
            }
        }
    }

    /** 当前档位的退避秒数，仅日志用 */
    private long currentBackoffSeconds() {
        long errs = Math.max(1, consecutiveErrors.get());
        int idx = (int) Math.min(errs, BACKOFF_STEPS_MS.length - 1);
        return BACKOFF_STEPS_MS[idx] / 1000;
    }

    /**
     * 拉取器运行状态快照（只读诊断，供 GET /chatbot/status）。
     * 不含消息内容，不含凭证明文；openKfid 打码。
     */
    public java.util.Map<String, Object> snapshotStatus() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        ChatbotFlowConfig.WechatKfConfig kf =
                flowRegistry.getConfig() != null ? flowRegistry.getConfig().getWechatKf() : null;
        boolean configured = kf != null && !isBlank(kf.getCorpId()) && !isBlank(kf.getSecret());
        m.put("configured", configured);
        m.put("pullModeEnabled", kf != null && kf.isPullModeEnabled());
        m.put("openKfidMasked", kf == null ? null : mask(kf.getOpenKfid()));
        m.put("lastErrcode", lastErrcode);
        m.put("lastAttemptAtMs", lastAttemptAtMs);
        m.put("lastSuccessAtMs", lastSuccessAtMs);
        m.put("consecutiveErrors", consecutiveErrors.get());
        m.put("backoffRemainingSeconds",
                Math.max(0, (backoffUntilMs.get() - System.currentTimeMillis()) / 1000));
        m.put("cursorInRedis", Boolean.TRUE.equals(stringRedisTemplate.hasKey(CURSOR_KEY)));
        m.put("tokenInRedis", tokenService.hasCachedToken());
        // true = 回调通道存活（9 分钟内收到过带 Token 的推送）；一直 false = 回调没通，纯靠 30s 兜底轮询
        m.put("callbackTokenInRedis", Boolean.TRUE.equals(stringRedisTemplate.hasKey(CB_TOKEN_KEY)));
        return m;
    }

    /** openKfid 打码：前 8 位 + *** */
    private String mask(String v) {
        if (isBlank(v)) {
            return null;
        }
        return v.length() <= 8 ? "***" : v.substring(0, 8) + "***";
    }

    private void handleMsg(JSONObject msg) {
        String msgid = msg.getString("msgid");
        // msgid 去重（轮询间隔内同一批消息不会重复处理）：add 返回实际写入数，0 = 已存在 = 处理过
        if (msgid != null) {
            Long added = stringRedisTemplate.opsForSet().add(PROCESSED_KEY, msgid);
            if (added == null || added == 0) {
                return;
            }
            stringRedisTemplate.expire(PROCESSED_KEY, Duration.ofDays(1));
        }

        Integer origin = msg.getInteger("origin");
        String msgtype = msg.getString("msgtype");
        String openKfid = msg.getString("open_kfid");
        String externalUserId = msg.getString("external_userid");

        // 客户文本消息：走正常槽位填充链路
        if (origin != null && origin == ORIGIN_CUSTOMER
                && "text".equals(msgtype)
                && openKfid != null && externalUserId != null) {
            JSONObject text = msg.getJSONObject("text");
            String content = text != null ? text.getString("content") : null;
            if (content != null && !content.isBlank()) {
                orchestrator.receiveText(openKfid, externalUserId, content);
            }
            return;
        }

        // 事件类消息：客户进入会话（kf_enter_session）-> 机器人主动开场提问
        // 事件格式兼容两种报文：顶层 event_type 或 event.event_type
        if ("event".equals(msgtype) && openKfid != null && externalUserId != null) {
            String eventType = msg.getString("event_type");
            JSONObject event = msg.getJSONObject("event");
            if (eventType == null && event != null) {
                eventType = event.getString("event_type");
            }
            if ("kf_enter_session".equals(eventType)) {
                log.info("[chatbot] 客户进入会话: openKfid={}, externalUserId={}, scene={}",
                        openKfid, externalUserId,
                        event != null ? event.getString("scene") : null);
                orchestrator.onCustomerEnter(openKfid, externalUserId);
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
