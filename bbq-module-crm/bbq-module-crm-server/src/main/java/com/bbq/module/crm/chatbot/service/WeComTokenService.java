package com.bbq.module.crm.chatbot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bbq.module.crm.chatbot.config.ChatbotFlowRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 企业微信 access_token 获取与缓存：
 * GET https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=xx&corpsecret=xx
 * token 有效期 7200s，缓存 Redis 并提前 5 分钟刷新；失效时调用方可通过 invalidate() 强制重取。
 */
@Slf4j
@Service
public class WeComTokenService {

    private static final String TOKEN_CACHE_KEY = "chatbot:wecom:access_token";
    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChatbotFlowRegistry flowRegistry;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * @return 有效的 access_token；corpId/secret 未配置或获取失败时返回 null（调用方降级处理）
     */
    public String getAccessToken() {
        String cached = stringRedisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        var kf = flowRegistry.getConfig() != null ? flowRegistry.getConfig().getWechatKf() : null;
        if (kf == null || isBlank(kf.getCorpId()) || isBlank(kf.getSecret())) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(GET_TOKEN_URL, kf.getCorpId(), kf.getSecret())))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject result = JSON.parseObject(resp.body());
            if (result.getIntValue("errcode") != 0) {
                log.error("[chatbot] 获取 access_token 失败: {}", resp.body());
                return null;
            }
            String token = result.getString("access_token");
            long expiresIn = result.getLongValue("expires_in");
            // 提前 5 分钟过期，避免边界时刻调用失败
            stringRedisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token,
                    Duration.ofSeconds(Math.max(60, expiresIn - 300)));
            log.info("[chatbot] access_token 已刷新");
            return token;
        } catch (Exception e) {
            log.error("[chatbot] 获取 access_token 异常", e);
            return null;
        }
    }

    /** token 被微信侧判定失效（40014/42001）时调用，下次强制重新获取 */
    public void invalidate() {
        stringRedisTemplate.delete(TOKEN_CACHE_KEY);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
