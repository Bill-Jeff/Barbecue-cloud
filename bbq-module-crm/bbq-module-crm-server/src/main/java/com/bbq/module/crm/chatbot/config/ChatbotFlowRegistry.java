package com.bbq.module.crm.chatbot.config;

import com.alibaba.fastjson2.JSON;
import com.bbq.module.crm.chatbot.model.ChatbotFlowConfig;
import com.bbq.module.crm.chatbot.model.FlowDefinition;
import com.bbq.module.crm.chatbot.model.StepDefinition;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * 流程配置装载器：
 * 启动时从 classpath:chatbot-flow.yaml 加载全部流程定义，构建 flowId -> FlowDefinition 的内存索引。
 * 后续要做成 DB 配置表动态加载时，只需替换本类的 load() 实现，引擎层无感知。
 */
@Slf4j
@Component
public class ChatbotFlowRegistry {

    private static final String CONFIG_LOCATION = "chatbot-flow.yaml";

    @Getter
    private ChatbotFlowConfig config;

    /** flowId -> FlowDefinition */
    private final Map<String, FlowDefinition> flowMap = new ConcurrentHashMap<>();

    @Resource
    private org.springframework.core.io.ResourceLoader resourceLoader;

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载流程配置（预留管理端修改配置后的热刷新入口）
     * 解析策略：先按通用 Map 读 YAML -> 若存在 chatbot: 根包裹层则自动解包
     * -> fastjson2 转 ChatbotFlowConfig。两种根层级写法均可加载。
     */
    public synchronized void reload() {
        try (InputStream in = resourceLoader.getResource("classpath:" + CONFIG_LOCATION).getInputStream()) {
            Object root = new Yaml().load(in);
            if (root instanceof Map<?, ?> map && map.containsKey("chatbot")) {
                root = map.get("chatbot");
            }
            if (root == null) {
                log.warn("[chatbot] {} 为空，未加载任何流程", CONFIG_LOCATION);
                return;
            }
            ChatbotFlowConfig loaded = JSON.to(ChatbotFlowConfig.class, root);
            this.config = loaded;
            this.flowMap.clear();
            for (FlowDefinition flow : loaded.getFlows()) {
                validate(flow);
                flowMap.put(flow.getId(), flow);
            }
            log.info("[chatbot] 流程配置加载完成，flows={}", flowMap.keySet());
        } catch (Exception e) {
            throw new IllegalStateException("[chatbot] 加载 " + CONFIG_LOCATION + " 失败", e);
        }
    }

    private void validate(FlowDefinition flow) {
        if (flow.getId() == null || flow.getId().isBlank()) {
            throw new IllegalStateException("流程缺少 id");
        }
        Map<String, StepDefinition> stepIds = new ConcurrentHashMap<>();
        for (StepDefinition step : flow.getSteps()) {
            if (step.getStepId() == null || stepIds.putIfAbsent(step.getStepId(), step) != null) {
                throw new IllegalStateException("流程 " + flow.getId() + " 中 stepId 缺失或重复: " + step.getStepId());
            }
        }
        if (!stepIds.containsKey(flow.getEntryStepId())) {
            throw new IllegalStateException("流程 " + flow.getId() + " 的 entryStepId 不存在: " + flow.getEntryStepId());
        }
        for (StepDefinition step : flow.getSteps()) {
            if (step.getNextStepId() != null && !stepIds.containsKey(step.getNextStepId())) {
                throw new IllegalStateException("流程 " + flow.getId() + " 步骤 " + step.getStepId()
                        + " 的 nextStepId 不存在: " + step.getNextStepId());
            }
        }
    }

    public FlowDefinition getFlow(String flowId) {
        return flowMap.get(flowId);
    }

    /** 当前默认流程：多流程场景可按入口来源（openKfid/关键词）路由，骨架先取第一个 */
    public FlowDefinition getDefaultFlow() {
        return flowMap.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("[chatbot] 未配置任何流程"));
    }

}
