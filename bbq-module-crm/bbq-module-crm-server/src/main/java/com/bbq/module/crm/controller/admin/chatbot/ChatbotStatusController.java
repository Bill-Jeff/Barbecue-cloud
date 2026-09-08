package com.bbq.module.crm.controller.admin.chatbot;

import com.bbq.module.crm.chatbot.service.WechatKfMessagePuller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信客服机器人「运行状态」只读诊断接口——部署/上线验证用，不用翻日志。
 *
 * <p>GET /chatbot/status 返回：
 * <ul>
 *   <li>configured / pullModeEnabled / openKfidMasked：配置是否加载、拉取模式是否开启</li>
 *   <li>lastErrcode / lastAttemptAt / lastSuccessAt：最近一次 sync_msg 结果与时间</li>
 *   <li>consecutiveErrors / backoffRemainingSeconds：当前退避状态</li>
 *   <li>cursorInRedis / tokenInRedis：两把关键 key 是否在（部署后应为 true；全 false 说明配置没生效或 Redis 不通）</li>
 * </ul>
 *
 * <p>只读、无消息内容、无凭证明文；如需收紧可在类上去掉 {@code @PermitAll} 走登录态。
 */
@Slf4j
@RestController
@RequestMapping("/chatbot")
public class ChatbotStatusController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Resource
    private WechatKfMessagePuller puller;

    @GetMapping("/status")
    @PermitAll
    public Map<String, Object> status() {
        Map<String, Object> data = puller.snapshotStatus();
        // 时间戳附人类可读格式（0 表示还没有过尝试/成功）
        data.put("lastAttemptAt", fmt(data.remove("lastAttemptAtMs")));
        data.put("lastSuccessAt", fmt(data.remove("lastSuccessAtMs")));
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("data", data);
        return r;
    }

    private String fmt(Object v) {
        long ms = v instanceof Long l ? l : 0L;
        return ms <= 0 ? "-" : FMT.format(Instant.ofEpochMilli(ms));
    }
}
