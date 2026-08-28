package com.bbq.module.flashsale.mq.consumer;

import cn.hutool.crypto.SecureUtil;
import com.bbq.module.flashsale.mq.message.FlashSaleOrderMessage;
import com.bbq.module.flashsale.service.FlashSaleRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class FlashSaleDlxConsumer {

    /**
     * 死信补偿幂等键前缀：消费端 ack 前崩溃会导致消息重投，必须保证同一消息只补偿一次
     */
    private static final String DLX_COMPENSATE_KEY_PREFIX = "flashsale:dlx:compensate:";
    private static final Duration COMPENSATE_KEY_TTL = Duration.ofHours(24);

    private final ObjectMapper objectMapper;
    private final FlashSaleRedisService flashSaleRedisService;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "${bbqmq.rabbitmq.flash-sale.dlx.queue:flash.sale.dlx.queue}")
    public void handleDlxMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        // 幂等去重：优先用消息自带 messageId，缺失时用 body 摘要
        String msgId = message.getMessageProperties().getMessageId();
        String dedupKey = DLX_COMPENSATE_KEY_PREFIX + (msgId != null ? msgId : SecureUtil.md5(body));
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", COMPENSATE_KEY_TTL);
        if (!Boolean.TRUE.equals(first)) {
            log.warn("死信补偿已处理过，跳过: dedupKey={}", dedupKey);
            return;
        }

        try {
            FlashSaleOrderMessage orderMessage = objectMapper.readValue(message.getBody(), FlashSaleOrderMessage.class);
            if (orderMessage.getFlashSaleId() != null && orderMessage.getQuantity() > 0) {
                flashSaleRedisService.incrementStock(orderMessage.getFlashSaleId(), orderMessage.getQuantity());
                log.warn("秒杀订单重试耗尽，已回补 Redis 库存: flashSaleId={}, quantity={}",
                        orderMessage.getFlashSaleId(), orderMessage.getQuantity());
            } else if (orderMessage.getRequestId() != null) {
                log.error("秒杀订单死信缺少有效库存信息，无法自动补偿: requestId={}, body={}",
                        orderMessage.getRequestId(), body);
            } else {
                // 支付消息只包含 orderId；Redis 待发送集合会继续补发，这里不能回补库存。
                log.error("扣款消息进入死信队列，将由待发送任务继续补发: body={}", body);
            }
        } catch (Exception ex) {
            // 反序列化失败通常是扣款消息（UserUpdateBalanceMessage）：订单可能已创建、扣款未完成，
            // 此时自动回补库存会造成「有单无款」，必须人工介入，绝不能回补。
            log.error("死信消息非订单创建消息，无法自动补偿，需人工处理: body={}", body);
            // TODO: 落库失败记录表 + 接入钉钉/邮件告警，供人工介入
        }
    }
}
