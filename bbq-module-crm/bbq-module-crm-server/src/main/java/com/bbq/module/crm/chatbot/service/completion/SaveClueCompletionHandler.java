package com.bbq.module.crm.chatbot.service.completion;

import com.bbq.module.crm.chatbot.model.ChatSession;
import com.bbq.module.crm.chatbot.model.FlowDefinition;
import com.bbq.module.crm.entity.CrmClue;
import com.bbq.module.crm.service.CrmClueService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 示例完成动作：把收集到的槽位落成一条线索（复用现有 CrmClueService）
 * 槽位 key 约定：name -> 线索名，phone -> 手机号，与 chatbot-flow.yaml 对应
 */
@Slf4j
@Component
public class SaveClueCompletionHandler implements CompletionActionHandler {

    public static final String ACTION = "save_clue";

    @Resource
    private CrmClueService crmClueService;

    @Override
    public String action() {
        return ACTION;
    }

    @Override
    public void handle(ChatSession session, FlowDefinition flow) {
        Map<String, Object> slots = session.getSlots();
        CrmClue clue = new CrmClue();
        clue.setName(str(slots.get("name"), "微信客户"));
        String mobile = str(slots.get("phone"), "");
        clue.setMobile(mobile);
        if (!mobile.isBlank()) {
            clue.setMobileMd5(md5Hex(mobile));
        }
        clue.setCreateTime(LocalDateTime.now());
        try {
            crmClueService.saveClue(clue);
            log.info("[chatbot] 会话 {} 流程 {} 完成，线索已落库", session.getSessionId(), flow.getId());
        } catch (Exception e) {
            // 落库失败不影响对话收尾，骨架阶段仅记录；生产建议投递 MQ 补偿
            log.error("[chatbot] 会话 {} 线索落库失败: {}", session.getSessionId(), slots, e);
        }
    }

    private String str(Object value, String defaultVal) {
        return value == null ? defaultVal : String.valueOf(value);
    }

    private String md5Hex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
