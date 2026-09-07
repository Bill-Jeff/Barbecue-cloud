package com.bbq.module.crm.chatbot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * chatbot-flow.yaml 根配置对象（SnakeYAML 绑定）
 */
@Data
public class ChatbotFlowConfig {

    /** 会话在 Redis 中的存活时长（分钟），超时未回复则过期重新开始 */
    private int sessionTtlMinutes = 30;

    /** 微信客服（企业微信客服）相关配置 */
    private WechatKfConfig wechatKf = new WechatKfConfig();

    /** 流程定义列表 */
    private List<FlowDefinition> flows = new ArrayList<>();

    @Data
    public static class WechatKfConfig {
        /** 企业 CorpID */
        private String corpId;
        /** 回调 URL 校验用的 Token */
        private String token;
        /** 消息加解密 EncodingAESKey（43 位，Base64 时补一个 = ） */
        private String encodingAesKey;
        /** 客服应用 secret（用于取 access_token，骨架阶段可先占位） */
        private String secret;

        /**
         * 拉取模式要服务的客服账号 open_kfid（wk 开头）。
         * sync_msg 按账号拉取：不配置时请求体不带 open_kfid，应用当前无任何授权账号会报
         * 95000 invalid open_kfid；配置了但该账号未把本应用设为「可调用 API 的应用」会报
         * 60030 / 48007。账号列表可用 GET /cgi-bin/kf/account/list 查询。
         */
        private String openKfid;

        /**
         * 是否启用「拉取模式」sync_msg 5s 轮询。
         * 默认 true（保留历史行为）；当 callback URL 已跑通、消息全走企微回调时，
         * 设为 false 可以关闭 sync_msg，避免「回调 + 拉取」双通道重复处理同一批消息。
         * 关闭后 corpId/secret 仍然保留（send_msg 仍需要用它取 access_token 真实下发）。
         */
        private boolean pullModeEnabled = true;
    }
}
