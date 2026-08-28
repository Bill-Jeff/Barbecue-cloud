package com.bbq.module.flashsale.mq.producer;

import com.bbq.module.flashsale.mq.message.FlashSaleOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 主交换机与订单创建路由键，外部化到 bbqmq.rabbitmq.flash-sale.* 配置
     */
    @Value("${bbqmq.rabbitmq.flash-sale.exchange:flash.sale.exchange}")
    private String exchange;
    @Value("${bbqmq.rabbitmq.flash-sale.order.routing-key:flash.sale.order.create}")
    private String routingKey;

    public void sendOrder(FlashSaleOrderMessage msg) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        log.info("秒杀订单入队: flashSaleId={}, quantity={}", msg.getFlashSaleId(), msg.getQuantity());
    }
}
