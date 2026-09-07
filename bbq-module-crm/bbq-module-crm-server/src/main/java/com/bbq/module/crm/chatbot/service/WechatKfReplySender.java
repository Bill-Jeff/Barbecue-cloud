package com.bbq.module.crm.chatbot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信客服消息发送器：
 * POST https://qyapi.weixin.qq.com/cgi-bin/kf/send_msg?access_token=xxx
 *
 * token 由 WeComTokenService 统一管理（Redis 缓存、提前刷新、失效自动重取）；
 * corpId/secret 未配置时降级为 MOCK 日志，不阻塞对话状态机本地调试。
 */
@Slf4j
@Service
public class WechatKfReplySender {

    private static final String SEND_MSG_URL = "https://qyapi.weixin.qq.com/cgi-bin/kf/send_msg?access_token=";

    @Resource
    private WeComTokenService tokenService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 发送文本消息给客户
     * @param openKfid       客服帐号 ID
     * @param externalUserId 客户 userid
     */
    public void sendText(String openKfid, String externalUserId, String content) {
        // 防御：企微 send_msg body 走严格 schema 校验，errcode=40096 = "invalid string value"，
        // 通常是某个 string 字段（如 touser/open_kfid）被 fastjson 输出成无引号数字触发的。
        // 上游任何一环（callback JSON 解析、模拟器、Puller sync_msg）把 ID 解析成
        // Integer/Long 后传到这里的，都会引发这个错。这里统一 String.valueOf 兜底，
        // 保证 JSON 输出永远是带引号的字符串，与企微 schema 匹配。
        String safeTouser = externalUserId == null ? null : String.valueOf(externalUserId);
        String safeOpenKfid = openKfid == null ? null : String.valueOf(openKfid);
        String safeContent = content == null ? null : String.valueOf(content);

        String accessToken = tokenService.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            // 未配置凭证：MOCK 发送，方便本地联调观察话术
            log.info("[chatbot][MOCK 发送] open_kfid={}, toUser={}, content={}", safeOpenKfid, safeTouser, safeContent);
            return;
        }
        // fastjson 的 JSONObject(Map) 构造会把传入的 Map 当底层存储直接装进去；
        // 上一版用的 Map.of(...) 返回的是 JDK 不可变 Map (ImmutableCollections$MapN)，
        // 一旦再 .put() 就抛 UnsupportedOperationException —— 这是生产报错的根因。
        // 改用「无参构造 + 逐个 put」，底层是 LinkedHashMap，put 安全。
        JSONObject body = new JSONObject();
        body.put("touser", safeTouser);
        body.put("msgtype", "text");
        body.put("open_kfid", safeOpenKfid);
        JSONObject text = new JSONObject();
        text.put("content", safeContent);
        body.put("text", text);

        JSONObject result = doSend(accessToken, body, safeTouser);
        int errcode = result == null ? -1 : result.getIntValue("errcode");
        // token 失效：强制重取后重试一次
        if (errcode == 42001 || errcode == 40014) {
            tokenService.invalidate();
            String freshToken = tokenService.getAccessToken();
            if (freshToken != null) {
                result = doSend(freshToken, body, safeTouser);
                errcode = result == null ? -1 : result.getIntValue("errcode");
            }
        }
        if (errcode == 60020) {
            log.error("[chatbot] 发送失败：出口 IP 未加入企微「企业可信IP」白名单");
        } else if (errcode != 0) {
            log.error("[chatbot] 发送客服消息失败, toUser={}, resp={}", safeTouser, result);
        }
    }

    private JSONObject doSend(String accessToken, JSONObject body, String externalUserId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEND_MSG_URL + accessToken))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JSON.parseObject(resp.body());
        } catch (Exception e) {
            log.error("[chatbot] 发送客服消息异常, toUser={}", externalUserId, e);
            return null;
        }
    }
}
