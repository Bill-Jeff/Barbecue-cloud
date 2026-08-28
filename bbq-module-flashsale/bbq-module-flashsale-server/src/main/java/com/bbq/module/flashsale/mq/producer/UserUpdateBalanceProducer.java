package com.bbq.module.flashsale.mq.producer;

import com.bbq.module.flashsale.mq.message.UserUpdateBalanceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateBalanceProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 主交换机与扣款路由键，外部化到 bbqmq.rabbitmq.flash-sale.* 配置
     */
    @Value("${bbqmq.rabbitmq.flash-sale.exchange:flash.sale.exchange}")
    private String exchange;
    @Value("${bbqmq.rabbitmq.flash-sale.payment.routing-key:flash.sale.payment.create}")
    private String routingKey;

    public void updateBalance(UserUpdateBalanceMessage msg) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        log.info("秒杀扣款消息入队: orderId={}", msg.getOrderId());
    }
}
