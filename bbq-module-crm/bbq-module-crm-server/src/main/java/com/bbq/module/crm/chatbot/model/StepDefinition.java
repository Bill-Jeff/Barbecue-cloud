package com.bbq.module.crm.chatbot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程中的一个步骤：机器人发一个问题，等待用户填入本步骤的槽位
 */
@Data
public class StepDefinition {

    /** 步骤唯一标识，流程内不可重复，如 ask_name */
    private String stepId;

    /** 步骤名称（展示用） */
    private String stepName;

    /** 机器人首次发问的话术 */
    private String question;

    /** 本步骤要收集的槽位（通常 1 个） */
    private List<SlotDefinition> slots = new ArrayList<>();

    /** 本步骤完成后跳转的下一步 stepId；为空表示流程结束 */
    private String nextStepId;
}
