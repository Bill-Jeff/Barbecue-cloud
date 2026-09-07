package com.bbq.module.crm.chatbot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条完整的信息收集流程，例如「线索收集」：
 * 称呼 -> 手机号 -> 需求 -> 预算 -> 完成
 */
@Data
public class FlowDefinition {

    /** 流程唯一标识，如 clue_collect */
    private String id;

    /** 流程名称（展示用） */
    private String name;

    /** 入口步骤 stepId */
    private String entryStepId;

    /** 步骤列表，按定义顺序执行（实际跳转由 nextStepId 决定） */
    private List<StepDefinition> steps = new ArrayList<>();

    /** 流程全部完成后发送的结束语，支持 {slots} 占位 */
    private String completeReply = "信息已收集完成，感谢您的配合！";

    /** 完成后触发的动作标识，对应 CompletionActionHandler#action()，可为空 */
    private String onCompleteAction;
}
