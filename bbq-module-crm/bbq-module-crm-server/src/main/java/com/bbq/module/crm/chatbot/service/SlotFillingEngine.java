package com.bbq.module.crm.chatbot.service;

import com.bbq.module.crm.chatbot.model.ChatSession;
import com.bbq.module.crm.chatbot.model.FlowDefinition;
import com.bbq.module.crm.chatbot.model.SlotDefinition;
import com.bbq.module.crm.chatbot.model.StepDefinition;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 槽位填充引擎（状态机的"转移函数"）：
 *
 * 输入：当前会话状态 + 用户本轮消息
 * 输出：机器人要回复的话术列表（0~N 条）
 *
 * 一轮的逻辑：
 * 1. 会话还没进入入口步骤 -> 进入入口步骤，发问
 * 2. 当前步骤有槽位未填 -> 从本轮输入提取；提取失败则追问（带重试上限）
 * 3. 当前步骤槽位填齐 -> 跳转 nextStepId 发问；没有下一步 -> 流程完成，触发完成动作
 */
@Slf4j
@Component
public class SlotFillingEngine {

    @Resource
    private SlotExtractor slotExtractor;

    /**
     * 客户进入会话（kf_enter_session 事件）：机器人主动开场。
     * - 新会话：发入口步骤的问题（即"客户一进来机器人先开口"）
     * - 进行中的会话：重发当前问题（客户中途退出再进来，提醒他答到哪了）
     * - 已完成的会话：不打扰
     */
    public List<String> handleEnterEvent(ChatSession session, FlowDefinition flow) {
        List<String> replies = new ArrayList<>();
        if (session.isFinished()) {
            return replies;
        }
        if (session.getCurrentStepId() == null) {
            StepDefinition entry = flow.getSteps().stream()
                    .filter(s -> s.getStepId().equals(flow.getEntryStepId()))
                    .findFirst().orElseThrow();
            session.setCurrentStepId(entry.getStepId());
            replies.add(entry.getQuestion());
        } else {
            StepDefinition current = flow.getSteps().stream()
                    .filter(s -> s.getStepId().equals(session.getCurrentStepId()))
                    .findFirst().orElse(null);
            if (current != null) {
                replies.add(current.getQuestion());
            }
        }
        return replies;
    }

    public List<String> handleMessage(ChatSession session, FlowDefinition flow, String userText) {
        List<String> replies = new ArrayList<>();

        // 会话已结束：不再追问，提示客户重新开始
        if (session.isFinished()) {
            replies.add("本条信息已收集完成。如需重新咨询，请回复\"重新开始\"。");
            return replies;
        }

        // 0. 支持"重新开始"指令
        if (userText != null && userText.contains("重新开始")) {
            resetToEntry(session, flow);
            replies.add(flow.getSteps().stream()
                    .filter(s -> s.getStepId().equals(flow.getEntryStepId()))
                    .findFirst().map(StepDefinition::getQuestion).orElse(""));
            return replies;
        }

        // 1. 尚未进入入口步骤
        if (session.getCurrentStepId() == null) {
            StepDefinition entry = flow.getSteps().stream()
                    .filter(s -> s.getStepId().equals(flow.getEntryStepId()))
                    .findFirst().orElseThrow();
            session.setCurrentStepId(entry.getStepId());
            replies.add(entry.getQuestion());
            return replies;
        }

        // 2. 处理当前步骤
        StepDefinition current = flow.getSteps().stream()
                .filter(s -> s.getStepId().equals(session.getCurrentStepId()))
                .findFirst().orElse(null);
        if (current == null) {
            log.error("[chatbot] 会话 {} 指向了不存在的步骤 {}，重置流程", session.getSessionId(), session.getCurrentStepId());
            resetToEntry(session, flow);
            replies.add("对话状态异常，已重新开始。" + firstQuestion(flow));
            return replies;
        }

        boolean stepFilled = tryFillSlots(session, current, userText, replies);
        if (!stepFilled) {
            return replies; // 还在追问，流程不推进
        }

        // 3. 当前步骤填齐，推进
        if (current.getNextStepId() != null) {
            StepDefinition next = flow.getSteps().stream()
                    .filter(s -> s.getStepId().equals(current.getNextStepId()))
                    .findFirst().orElseThrow();
            session.setCurrentStepId(next.getStepId());
            replies.add(next.getQuestion());
            return replies;
        }

        // 4. 没有下一步 -> 流程完成
        session.setFinished(true);
        session.setCurrentStepId(null);
        replies.add(renderTemplate(flow.getCompleteReply(), session.getSlots()));
        return replies;
    }

    /** 完成话术里的 {slotKey} 占位符替换，如 "好的 {name}，信息已收到！" */
    private String renderTemplate(String template, java.util.Map<String, Object> slots) {
        if (template == null || !template.contains("{")) {
            return template;
        }
        String result = template;
        for (java.util.Map.Entry<String, Object> e : slots.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }

    /**
     * 尝试用本轮输入填充当前步骤的槽位
     * @return true=本步骤槽位全部就绪（可推进），false=仍在追问
     */
    private boolean tryFillSlots(ChatSession session, StepDefinition step, String userText, List<String> replies) {
        List<SlotDefinition> slots = step.getSlots();
        // 只针对第一个"还没拿到值"的槽位做提取（严格逐槽位追问，保证一轮只问一件事）
        SlotDefinition pending = slots.stream()
                .filter(s -> !session.getSlots().containsKey(s.getKey()))
                .findFirst().orElse(null);

        if (pending != null) {
            String value = slotExtractor.extract(pending, userText);
            if (value != null) {
                session.getSlots().put(pending.getKey(), value);
                session.getSlotRetryCount().remove(pending.getKey());
            } else if (pending.isRequired()) {
                int retried = session.getSlotRetryCount().merge(pending.getKey(), 1, Integer::sum);
                if (retried >= pending.getMaxRetry()) {
                    // 降级：超过追问上限，记录空值放行，避免客户被卡死
                    log.warn("[chatbot] 会话 {} 槽位 {} 超过最大追问次数，放行空值",
                            session.getSessionId(), pending.getKey());
                    session.getSlots().put(pending.getKey(), "");
                } else {
                    String prompt = pending.getRetryPrompt() != null && !pending.getRetryPrompt().isBlank()
                            ? pending.getRetryPrompt()
                            : step.getQuestion();
                    replies.add(prompt);
                    return false;
                }
            } else {
                // 非必填提取不到 -> 跳过
                session.getSlots().put(pending.getKey(), "");
            }
        }

        // 本步骤是否还有未填槽位
        boolean allFilled = slots.stream().allMatch(s -> session.getSlots().containsKey(s.getKey()));
        if (!allFilled) {
            // 同一步骤多槽位场景：继续追问下一个
            SlotDefinition next = slots.stream()
                    .filter(s -> !session.getSlots().containsKey(s.getKey()))
                    .findFirst().orElseThrow();
            replies.add(next.getRetryPrompt() != null && !next.getRetryPrompt().isBlank()
                    ? next.getRetryPrompt() : step.getQuestion());
            return false;
        }
        return true;
    }

    private void resetToEntry(ChatSession session, FlowDefinition flow) {
        session.setCurrentStepId(null);
        session.setFinished(false);
        session.getSlots().clear();
        session.getSlotRetryCount().clear();
    }

    private String firstQuestion(FlowDefinition flow) {
        return flow.getSteps().stream()
                .filter(s -> s.getStepId().equals(flow.getEntryStepId()))
                .findFirst().map(StepDefinition::getQuestion).orElse("");
    }
}
