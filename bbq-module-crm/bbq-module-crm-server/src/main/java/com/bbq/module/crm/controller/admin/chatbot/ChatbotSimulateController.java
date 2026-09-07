package com.bbq.module.crm.controller.admin.chatbot;

import com.bbq.module.crm.chatbot.service.ChatbotOrchestrator;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信客服机器人「本地模拟器」——绕过真实微信链路，直调 ChatbotOrchestrator。
 *
 * <p>使用场景：
 * <ul>
 *   <li>微信客服被封 / 企业未备案 / 不想走真实 send_msg 时，端到端验证状态机</li>
 *   <li>槽位填充、追问、完成落库等业务逻辑本地联调</li>
 * </ul>
 *
 * <p>使用方式（IDEA 默认 profile=local 直接生效）：
 * <pre>
 * # 1. 触发客户进入会话（开场提问）
 * curl -X POST http://localhost:48086/chatbot/simulate/enter \
 *   -H "Content-Type: application/json" \
 *   -d '{"openKfid":"wk...","externalUserId":"mock_user_001"}'
 *
 * # 2. 模拟客户回复
 * curl -X POST http://localhost:48086/chatbot/simulate/text \
 *   -H "Content-Type: application/json" \
 *   -d '{"openKfid":"wk...","externalUserId":"mock_user_001","text":"张三"}'
 * </pre>
 *
 * <p>注意事项：
 * <ol>
 *   <li>仅 {@code @Profile("local")} 加载，prod 不暴露</li>
 *   <li>externalUserId 用 mock 串（如 mock_user_001），不会撞真客户</li>
 *   <li>secret 配了的情况下，send_msg 会真发（mock 串必 40096），仅日志报错不影响状态机</li>
 *   <li>想纯 MOCK 观察话术，临时把 chatbot-flow.yaml 里 secret 清空，sender 自动走 MOCK 日志分支</li>
 *   <li>不同 mock 串互不干扰（Redis 状态机 key 含 externalUserId）</li>
 * </ol>
 */
@Slf4j
@Profile("local")
@RestController
@RequestMapping("/chatbot/simulate")
public class ChatbotSimulateController {

    @Resource
    private ChatbotOrchestrator orchestrator;

    /**
     * 模拟客户进入会话：触发开场提问（kf_enter_session 流程）
     */
    @PostMapping("/enter")
    @PermitAll
    public Map<String, Object> simulateEnter(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return error("body 不能为空");
        }
        String openKfid = body.get("openKfid");
        String externalUserId = body.get("externalUserId");
        if (isBlank(openKfid) || isBlank(externalUserId)) {
            return error("openKfid 和 externalUserId 都必填");
        }
        log.info("[chatbot][SIMULATE] 模拟客户进入会话: openKfid={}, externalUserId={}",
                openKfid, externalUserId);
        try {
            orchestrator.onCustomerEnter(openKfid, externalUserId);
            return ok("已触发开场提问");
        } catch (Exception e) {
            log.error("[chatbot][SIMULATE] onCustomerEnter 异常", e);
            return error("处理异常: " + e.getMessage());
        }
    }

    /**
     * 模拟客户发送文本：走槽位填充 / 追问 / 完成链路
     */
    @PostMapping("/text")
    @PermitAll
    public Map<String, Object> simulateText(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return error("body 不能为空");
        }
        String openKfid = body.get("openKfid");
        String externalUserId = body.get("externalUserId");
        String text = body.get("text");
        if (isBlank(openKfid) || isBlank(externalUserId) || isBlank(text)) {
            return error("openKfid / externalUserId / text 都必填");
        }
        log.info("[chatbot][SIMULATE] 模拟客户文本: openKfid={}, externalUserId={}, text='{}'",
                openKfid, externalUserId, text);
        try {
            orchestrator.receiveText(openKfid, externalUserId, text);
            return ok("已送入状态机");
        } catch (Exception e) {
            log.error("[chatbot][SIMULATE] receiveText 异常", e);
            return error("处理异常: " + e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private Map<String, Object> ok(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("msg", msg);
        return r;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 400);
        r.put("msg", msg);
        return r;
    }
}
