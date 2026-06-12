package com.bbq.module.flashsale.producer;

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
        rabbitTemplate.convertAndSend(RabbitMQConfig.FLASH_SALE_QUEUE, msg);
        log.info("秒杀订单入队: flashSaleId={}, quantity={}", msg.getFlashSaleId(), msg.getQuantity());
    }
}
