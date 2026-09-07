package com.bbq.module.crm.chatbot.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 一次对话的会话状态，整体以 JSON 存 Redis：
 * key   = chatbot:session:{openKfid}:{externalUserId}
 * value = ChatSession JSON
 */
@Data
public class ChatSession {

    /** 会话唯一标识：{openKfid}:{externalUserId} */
    private String sessionId;

    /** 当前执行的流程 id */
    private String flowId;

    /** 当前所在步骤 stepId；null 表示尚未进入入口步骤 */
    private String currentStepId;

    /** 已收集到的槽位值：slotKey -> value */
    private Map<String, Object> slots = new HashMap<>();

    /** 各槽位已追问次数：slotKey -> count */
    private Map<String, Integer> slotRetryCount = new HashMap<>();

    /** 流程是否已完成 */
    private boolean finished;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最近一次更新时间 */
    private LocalDateTime updateTime;
}
