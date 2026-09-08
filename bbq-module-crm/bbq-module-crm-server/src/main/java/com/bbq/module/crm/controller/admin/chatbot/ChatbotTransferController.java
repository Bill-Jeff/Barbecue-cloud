package com.bbq.module.crm.controller.admin.chatbot;

import com.alibaba.fastjson2.JSONObject;
import com.bbq.module.crm.chatbot.service.WechatKfSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.util.HashMap;
import java.util.Map;

/**
 * 运营侧「人工接管」工具——无需走完线索收集，运营可以手工把任意客户的会话
 * 转给指定接待人员（POST /chatbot/transfer），或查询客户当前会话状态
 * （GET /chatbot/transfer-status）。两个接口只读 / 转接，均为低风险操作。
 *
 * <p>典型场景：
 * <ul>
 *   <li>机器人卡在某个 slot 想强切人工 → POST /transfer</li>
 *   <li>不确定客户当前在机器人手里还是人工手里 → GET /transfer-status</li>
 *   <li>测试服务上验证 service_state/trans 全链路（无需走完线索流程）</li>
 * </ul>
 *
 * <p>这两个接口与 /chatbot/status 一样是 @PermitAll（低风险诊断/运营工具），线上若要收紧可去掉。
 */
@Slf4j
@RestController
@RequestMapping("/chatbot")
public class ChatbotTransferController {

    @Resource
    private WechatKfSessionService sessionService;

    /**
     * 手工转人工：body {openKfid, externalUserId, servicerUserid? (可省，省则用 yaml 配置)}
     */
    @PostMapping("/transfer")
    @PermitAll
    public Map<String, Object> transfer(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return err("body 不能为空");
        }
        String openKfid = body.get("openKfid");
        String externalUserId = body.get("externalUserId");
        if (openKfid == null || openKfid.isBlank() || externalUserId == null || externalUserId.isBlank()) {
            return err("openKfid / externalUserId 必填");
        }
        String explicit = body.get("servicerUserid");
        try {
            boolean ok = sessionService.transferToServicer(openKfid, externalUserId, explicit);
            Map<String, Object> r = new HashMap<>();
            r.put("code", ok ? 0 : 400);
            r.put("msg", ok ? "已转人工（具体错误码见日志）" : "转接失败（具体原因见日志）");
            return r;
        } catch (Exception e) {
            log.error("[chatbot] 手工转人工异常", e);
            return err("处理异常: " + e.getMessage());
        }
    }

    /**
     * 查询客户当前会话状态：GET /chatbot/transfer-status?openKfid=wk...&externalUserId=wo...
     * 0 未处理 / 1 API 接待 / 2 待接入池 / 3 人工 / 4 已结束
     */
    @GetMapping("/transfer-status")
    @PermitAll
    public Map<String, Object> status(@RequestParam("openKfid") String openKfid,
                                       @RequestParam("externalUserId") String externalUserId) {
        JSONObject result = sessionService.getServiceState(openKfid, externalUserId);
        if (result == null) {
            return err("查询失败（access_token 不可用或网络异常）");
        }
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("data", result);
        return r;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 400);
        r.put("msg", msg);
        return r;
    }
}
