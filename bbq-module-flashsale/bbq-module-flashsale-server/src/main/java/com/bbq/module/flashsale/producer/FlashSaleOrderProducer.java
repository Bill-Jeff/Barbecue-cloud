package com.bbq.module.flashsale.producer;

import com.bbq.framework.mq.rabbitmq.config.BbqRabbitMQAutoConfiguration;
import com.bbq.module.flashsale.config.RabbitMQConfig;
import com.bbq.module.flashsale.entity.FlashSaleOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendOrder(FlashSaleOrderMessage msg) {
        // 发布到 Topic 交换机，由路由键 flash.sale.order.create 匹配绑定键 flash.sale.order.#
        rabbitTemplate.convertAndSend(
                BbqRabbitMQAutoConfiguration.FLASH_SALE_EXCHANGE,
                BbqRabbitMQAutoConfiguration.FLASH_SALE_ROUTING_KEY,
                msg);
        log.info("秒杀订单入队: flashSaleId={}, quantity={}", msg.getFlashSaleId(), msg.getQuantity());
    }
}
