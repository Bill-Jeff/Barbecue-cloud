package com.bbq.module.crm.chatbot.service.completion;

import com.bbq.module.crm.chatbot.model.ChatSession;
import com.bbq.module.crm.chatbot.model.FlowDefinition;

/**
 * 流程完成后的动作扩展点：
 * 在 chatbot-flow.yaml 的 flow.onCompleteAction 里填 action 标识，
 * Spring 容器里所有实现按 action() 匹配触发，可同时挂多个动作。
 */
public interface CompletionActionHandler {

    /** 对应 flow.onCompleteAction 的标识，如 save_clue */
    String action();

    void handle(ChatSession session, FlowDefinition flow);
}
