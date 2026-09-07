package com.bbq.module.crm.chatbot.service;

import com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry;
import com.bbq.module.crm.chatbot.model.ChatSession;
import com.bbq.module.crm.chatbot.model.FlowDefinition;
import com.bbq.module.crm.chatbot.service.completion.CompletionActionHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话编排器：把「取状态 -> 引擎推进 -> 回消息 -> 存状态 -> 完成动作」串成完整链路
 * 回调 Controller 只需调用 receiveText()
 */
@Slf4j
@Service
public class ChatbotOrchestrator {

    @Resource
    private ChatbotFlowRegistry flowRegistry;

    @Resource
    private ChatSessionRedisStore sessionStore;

    @Resource
    private SlotFillingEngine slotFillingEngine;

    @Resource
    private WechatKfReplySender replySender;

    @Resource
    private List<CompletionActionHandler> completionHandlers;

    /**
     * 处理客户发来的一条文本消息
     * @param openKfid       客服帐号 ID
     * @param externalUserId 客户 ID
     * @param userText       客户消息内容
     */
    public void receiveText(String openKfid, String externalUserId, String userText) {
        FlowDefinition flow = flowRegistry.getDefaultFlow();
        ChatSession session = sessionStore.getOrCreate(openKfid, externalUserId, flow.getId());
        boolean finishedBefore = session.isFinished();

        List<String> replies = slotFillingEngine.handleMessage(session, flow, userText);

        sessionStore.save(session);

        for (String reply : replies) {
            if (reply != null && !reply.isBlank()) {
                replySender.sendText(openKfid, externalUserId, reply);
            }
        }

        // 本轮刚好完成 -> 触发完成动作（避免重复触发，只在本轮从未完成变成完成时执行）
        if (session.isFinished() && !finishedBefore && flow.getOnCompleteAction() != null) {
            completionHandlers.stream()
                    .filter(h -> h.action().equals(flow.getOnCompleteAction()))
                    .forEach(h -> {
                        try {
                            h.handle(session, flow);
                        } catch (Exception e) {
                            log.error("[chatbot] 完成动作 {} 执行失败", h.action(), e);
                        }
                    });
        }
    }

    /**
     * 客户进入会话（kf_enter_session 事件）：机器人主动发起开场提问。
     * 注意企微的 48 小时窗口规则：进入会话后 48 小时内企业可主动发消息。
     */
    public void onCustomerEnter(String openKfid, String externalUserId) {
        FlowDefinition flow = flowRegistry.getDefaultFlow();
        ChatSession session = sessionStore.getOrCreate(openKfid, externalUserId, flow.getId());
        if (session.isFinished()) {
            return; // 已完成收集的客户不再打扰
        }
        List<String> replies = slotFillingEngine.handleEnterEvent(session, flow);
        if (replies.isEmpty()) {
            return;
        }
        sessionStore.save(session);
        for (String reply : replies) {
            if (reply != null && !reply.isBlank()) {
                replySender.sendText(openKfid, externalUserId, reply);
            }
        }
    }
}
