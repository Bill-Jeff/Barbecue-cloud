package com.bbq.module.crm.chatbot.enums;

/**
 * 槽位类型，SlotExtractor 按类型做提取与校验
 */
public interface SlotType {

    /** 任意文本 */
    String TEXT = "TEXT";

    /** 中国大陆手机号 */
    String PHONE = "PHONE";

    /** 数字（整数或小数，如预算） */
    String NUMBER = "NUMBER";

    /** 单选项，必须命中 options 之一（支持"1/2/3"序号或选项原文） */
    String CHOICE = "CHOICE";
}
