package com.bbq.module.crm.controller.admin.chatbot;

import com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry;
import com.bbq.module.crm.chatbot.service.WechatKfMessagePuller;
import com.bbq.module.crm.chatbot.util.WechatCallbackCrypto;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

import static com.bbq.module.crm.chatbot.util.WechatCallbackCrypto.extractXmlValue;

/**
 * 微信客服回调入口（企业微信客服/公众号客服通用）：
 * - GET  ：URL 校验（微信后台配置回调地址时触发）
 * - POST ：接收消息/事件推送
 *
 * 注意：该接口必须 PermitAll 且在网关/安全配置中放行；
 * 生产环境回调 URL 一般直连本服务（微信不都走网关路径），部署时注意路由。
 */
@Slf4j
@RestController
@RequestMapping("/chatbot/kf-callback")
public class WechatKfCallbackController {

    @Resource
    private ChatbotFlowRegistry flowRegistry;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private WechatKfMessagePuller puller;

    /**
     * 回调 URL 校验（企微/微信客服协议）：
     * GET 参数名是 msg_signature（不是公众号的 signature）；
     * msg_signature = sha1(sort(token, timestamp, nonce, echostr))；
     * echostr 为 AES 密文，验签通过后必须【解密并返回明文】，不能原样返回。
     */
    @GetMapping
    @PermitAll
    public String verify(@RequestParam(value = "msg_signature", required = false) String msgSignature,
                         @RequestParam(value = "signature", required = false) String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        var kf = flowRegistry.getConfig().getWechatKf();
        // 防坑：echostr 是 Base64，其中的 + 会被 @RequestParam 的 URL 解码变成空格，需要还原
        if (echostr != null) {
            echostr = echostr.replace(' ', '+');
        }
        String sign = msgSignature != null ? msgSignature : signature; // 兼容公众号风格参数名
        if (WechatCallbackCrypto.checkSignature(kf.getToken(), timestamp, nonce, echostr, sign)) {
            try {
                // 解密 echostr 并返回明文（企微要求）
                return WechatCallbackCrypto.decrypt(echostr, kf.getEncodingAesKey());
            } catch (Exception e) {
                log.error("[chatbot] echostr 解密失败，请检查 EncodingAESKey 是否与后台一致", e);
                return "fail";
            }
        }
        log.warn("[chatbot] 回调 URL 校验失败，请检查 Token 是否与后台一致");
        return "fail";
    }

    /**
     * 消息推送：企微安全模式下报文为 <Encrypt> 密文，先用 msg_signature 验签再解密。
     * body 可选：穿透服务的健康检查/探活会发空 body 的 POST，直接回 success 掉。
     */
    @PostMapping
    @PermitAll
    public String receive(@RequestParam(value = "msg_signature", required = false) String msgSignature,
                          @RequestParam(value = "signature", required = false) String signature,
                          @RequestParam(value = "timestamp", required = false) String timestamp,
                          @RequestParam(value = "nonce", required = false) String nonce,
                          @RequestBody(required = false) String xmlBody) {
        try {
            if (xmlBody == null || xmlBody.isBlank()) {
                log.info("[chatbot] 收到空 body 的 POST（穿透探活/健康检查），忽略");
                return "success";
            }
            String xml = xmlBody;
            String encrypt = extractXmlValue(xmlBody, "Encrypt");
            if (encrypt != null) {
                var kf = flowRegistry.getConfig().getWechatKf();
                String sign = msgSignature != null ? msgSignature : signature;
                // 安全校验：签名不对直接拒收（返回 success 防止微信重试轰炸）
                if (!WechatCallbackCrypto.checkSignature(kf.getToken(), timestamp, nonce, encrypt, sign)) {
                    log.warn("[chatbot] 回调消息验签失败，已丢弃");
                    return "success";
                }
                xml = WechatCallbackCrypto.decrypt(encrypt, kf.getEncodingAesKey());
            }

            String msgType = extractXmlValue(xml, "MsgType");
            String fromUser = extractXmlValue(xml, "FromUserName");
            String openKfid = firstNonNull(extractXmlValue(xml, "OpenKfId"), extractXmlValue(xml, "ToUserName"));

            // 回调推送里的 Token（10 分钟有效）：存 Redis 供 Puller 传给 sync_msg，
            // 解除「无 token 严格频率限制」；同时立即触发一次拉取（秒级响应，不再等下个轮询周期）。
            // 注意：消息本体仍由 puller 处理，这里绝不直接喂状态机（FromUserName 是昵称，会 40096）。
            String cbToken = extractXmlValue(xml, "Token");
            if (cbToken != null && !cbToken.isBlank()) {
                stringRedisTemplate.opsForValue().set(WechatKfMessagePuller.CB_TOKEN_KEY, cbToken, Duration.ofMinutes(9));
            }
            puller.triggerFromCallback();

            if ("text".equals(msgType)) {
                // 注意：callback XML 里的 FromUserName 在「客户=管理员本人」/ 加密模式下
                // 是客户微信昵称，不是 external_userid，直接喂给 orchestrator 会导致
                // send_msg 时 touser 不合法（errcode 40096）。这里只打日志，真正处理
                // 交给 WechatKfMessagePuller 走 kf/sync_msg 拿真实 external_userid。
                String content = extractXmlValue(xml, "Content");
                log.info("[chatbot] 收到客户文本（由 puller 走 sync_msg 处理）: openKfid='{}', fromUser='{}', content='{}'",
                        openKfid, fromUser, content);
            } else if ("event".equals(msgType)) {
                // 企微 kf_msg_or_event：XML 里只带 Token，真正消息由 puller 拉 sync_msg
                log.info("[chatbot] 收到事件推送（由 puller 走 sync_msg 处理）: Event={}, hasToken={}",
                        extractXmlValue(xml, "Event"),
                        extractXmlValue(xml, "Token") != null);
            } else {
                log.info("[chatbot] 忽略消息类型 MsgType={}", msgType);
            }
        } catch (Exception e) {
            // 微信要求异常时也不能反复重试压垮接口：记日志，返回 success 让微信停止重试
            log.error("[chatbot] 处理回调异常", e);
        }
        // 微信约定：必须回复（默认返回空串/"success"），否则会重试推送
        return "success";
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
