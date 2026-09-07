package com.bbq.module.crm.chatbot.service;

import com.bbq.module.crm.chatbot.enums.SlotType;
import com.bbq.module.crm.chatbot.model.SlotDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 槽位提取器：按槽位类型从用户原始输入里提取值。
 * 骨架阶段用正则 + 规则；后续可替换为 LLM 抽取（对引擎层透明）。
 */
@Slf4j
@Component
public class SlotExtractor {

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    /**
     * @return 提取成功返回归一化后的值；无法从本轮输入提取则返回 null
     */
    public String extract(SlotDefinition slot, String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }
        String text = userText.trim();
        return switch (slot.getType() == null ? SlotType.TEXT : slot.getType()) {
            case SlotType.PHONE -> extractPhone(text);
            case SlotType.NUMBER -> extractNumber(text);
            case SlotType.CHOICE -> extractChoice(slot, text);
            default -> text;
        };
    }

    private String extractPhone(String text) {
        // 容忍 "138 0013 8000"、"138-0013-8000" 之类写法
        String compact = text.replaceAll("[\\s\\-]", "");
        Matcher m = PHONE_PATTERN.matcher(compact);
        return m.find() ? m.group() : null;
    }

    private String extractNumber(String text) {
        Matcher m = NUMBER_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractChoice(SlotDefinition slot, String text) {
        List<String> options = slot.getOptions();
        if (options == null || options.isEmpty()) {
            return text;
        }
        // 1. 直接命中选项原文（contains 兼容 "选A" / "A选项"）
        for (String option : options) {
            if (text.equalsIgnoreCase(option) || text.contains(option)) {
                return option;
            }
        }
        // 2. 序号命中："1"、"2、"、"选 3"
        Matcher m = Pattern.compile("[123456789]").matcher(text);
        if (m.find()) {
            int idx = Integer.parseInt(m.group()) - 1;
            if (idx >= 0 && idx < options.size()) {
                return options.get(idx);
            }
        }
        return null;
    }
}
