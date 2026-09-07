package com.bbq.module.crm.chatbot.model;

import com.bbq.module.crm.chatbot.enums.SlotType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 槽位定义：一个待收集的字段
 */
@Data
public class SlotDefinition {

    /** 槽位 key，写入 session.slots 的字段名，如 name / phone / budget */
    private String key;

    /** 槽位中文名，用于追问话术，如 "手机号" */
    private String name;

    /** 槽位类型，见 SlotType */
    private String type = SlotType.TEXT;

    /** 是否必填（非必填槽位提取不到时直接跳过） */
    private boolean required = true;

    /** CHOICE 类型的候选项 */
    private List<String> options = new ArrayList<>();

    /** 校验/提取失败时的追问话术，为空则复用步骤 question */
    private String retryPrompt;

    /** 同一槽位最多追问次数，超过后走降级（放行并记录空值） */
    private int maxRetry = 3;
}
