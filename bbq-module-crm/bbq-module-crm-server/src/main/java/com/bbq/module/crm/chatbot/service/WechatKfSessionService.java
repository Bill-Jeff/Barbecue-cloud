package com.bbq.module.crm.chatbot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry;
import com.bbq.module.crm.chatbot.model.ChatbotFlowConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信客服会话管理：API 接待转人工。
 *
 * <p>机器人收集完线索后调用 service_state/trans 把会话转给人工接待人员，
 * 之后接待人员在企微客户端（工作台 → 微信客服）里就能看到该客户并直接回复；
 * 人工的回复仍能被 sync_msg 拉到（origin=5），可做留痕。
 *
 * <p>配置：chatbot-flow.yaml 的 wechatKf.transferServicerUserid（企微成员 userid）。
 * 留空则不转人工（机器人收尾后静默结束）。
 * 常见错误：95014 = 接待人员未在企业微信激活使用。
 */
@Slf4j
@Service
public class WechatKfSessionService {

    private static final String TRANS_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/kf/service_state/trans?access_token=";
    private static final String GET_STATE_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/kf/service_state/get?access_token=";

    @Resource
    private ChatbotFlowRegistry flowRegistry;

    @Resource
    private WeComTokenService tokenService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 把客户会话转给配置的人工接待人员（service_state -> 3 由人工接待）。
     *
     * @return true=转接成功；false=未配置/转接失败（原因见日志）
     */
    public boolean transferToServicer(String openKfid, String externalUserId) {
        return transferToServicer(openKfid, externalUserId, null);
    }

    /**
     * 转人工（运营侧可指定具体接待人员，未指定时用 yaml 配置的 transferServicerUserid）
     *
     * @param explicitServicer 非空时优先使用（如运营从后台指定了具体客服），否则读 yaml 配置
     */
    public boolean transferToServicer(String openKfid, String externalUserId, String explicitServicer) {
        ChatbotFlowConfig.WechatKfConfig kf =
                flowRegistry.getConfig() != null ? flowRegistry.getConfig().getWechatKf() : null;
        String servicer = (explicitServicer != null && !explicitServicer.isBlank())
                ? explicitServicer
                : (kf == null ? null : kf.getTransferServicerUserid());
        if (servicer == null || servicer.isBlank()) {
            log.info("[chatbot] 未配置 transferServicerUserid，会话不转人工（机器人收尾后结束）");
            return false;
        }
        String token = tokenService.getAccessToken();
        if (token == null) {
            log.error("[chatbot] 转人工失败：access_token 不可用");
            return false;
        }
        JSONObject body = new JSONObject();
        body.put("open_kfid", openKfid);
        body.put("external_userid", externalUserId);
        body.put("service_state", 3);
        body.put("servicer_userid", servicer);
        try {
            JSONObject result = doTrans(token, body);
            int errcode = result == null ? -1 : result.getIntValue("errcode");
            if (errcode == 42001 || errcode == 40014) {
                // token 失效：强制重取后重试一次
                tokenService.invalidate();
                String fresh = tokenService.getAccessToken();
                if (fresh != null) {
                    result = doTrans(fresh, body);
                    errcode = result == null ? -1 : result.getIntValue("errcode");
                }
            }
            if (errcode == 0) {
                log.info("[chatbot] 会话已转人工: openKfid={}, externalUserId={}, servicer={}",
                        openKfid, externalUserId, servicer);
                return true;
            }
            if (errcode == 95014) {
                log.error("[chatbot] 转人工失败 95014：接待人员 {} 未在企业微信激活，请先激活后再试", servicer);
            } else {
                log.error("[chatbot] 转人工失败 errcode={} body={}", errcode, result);
            }
            return false;
        } catch (Exception e) {
            log.error("[chatbot] 转人工调用异常", e);
            return false;
        }
    }

    private JSONObject doTrans(String token, JSONObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TRANS_URL + token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return JSON.parseObject(resp.body());
    }

    /**
     * 查询客户当前会话状态（service_state：0 未处理 / 1 API 接待 / 2 待接入池 / 3 人工 / 4 已结束）
     * 用于运营确认「这条客户现在到底在机器人手里还是人工手里」
     */
    public JSONObject getServiceState(String openKfid, String externalUserId) {
        String token = tokenService.getAccessToken();
        if (token == null) {
            return null;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("open_kfid", openKfid);
            body.put("external_userid", externalUserId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GET_STATE_URL + token))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JSON.parseObject(resp.body());
        } catch (Exception e) {
            log.error("[chatbot] 查询 service_state 异常", e);
            return null;
        }
    }
}
