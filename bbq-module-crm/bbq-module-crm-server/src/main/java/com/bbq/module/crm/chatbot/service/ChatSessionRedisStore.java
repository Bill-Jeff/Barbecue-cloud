package com.bbq.module.crm.chatbot.service;

import com.alibaba.fastjson2.JSON;
import com.bbq.module.crm.chatbot.model.ChatSession;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 会话状态机的 Redis 存取层：
 * key = chatbot:session:{sessionId}，value = ChatSession JSON，带 TTL 自动过期。
 * 过期即视为"客户流失本轮"：下次再发消息会重新开一轮流程。
 */
@Slf4j
@Service
public class ChatSessionRedisStore {

    private static final String KEY_PREFIX = "chatbot:session:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry flowRegistry;

    public String buildSessionId(String openKfid, String externalUserId) {
        return openKfid + ":" + externalUserId;
    }

    /** 读取会话；不存在则创建新会话（未进入入口步骤状态） */
    public ChatSession getOrCreate(String openKfid, String externalUserId, String flowId) {
        String key = KEY_PREFIX + buildSessionId(openKfid, externalUserId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            ChatSession session = JSON.parseObject(json, ChatSession.class);
            touch(key, session);
            return session;
        }
        ChatSession session = new ChatSession();
        session.setSessionId(buildSessionId(openKfid, externalUserId));
        session.setFlowId(flowId);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    /** 回写会话并刷新 TTL */
    public void save(ChatSession session) {
        session.setUpdateTime(LocalDateTime.now());
        int ttlMinutes = flowRegistry.getConfig() != null
                ? flowRegistry.getConfig().getSessionTtlMinutes() : 30;
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + session.getSessionId(),
                JSON.toJSONString(session),
                Duration.ofMinutes(ttlMinutes));
    }

    /** 客户明确说"不用了"或人工接管时，可主动清掉会话 */
    public void clear(String openKfid, String externalUserId) {
        stringRedisTemplate.delete(KEY_PREFIX + buildSessionId(openKfid, externalUserId));
    }

    private void touch(String key, ChatSession session) {
        // 读时顺带续期，避免客户正在回答时状态过期
        int ttlMinutes = flowRegistry.getConfig() != null
                ? flowRegistry.getConfig().getSessionTtlMinutes() : 30;
        stringRedisTemplate.expire(key, Duration.ofMinutes(ttlMinutes));
    }
}
