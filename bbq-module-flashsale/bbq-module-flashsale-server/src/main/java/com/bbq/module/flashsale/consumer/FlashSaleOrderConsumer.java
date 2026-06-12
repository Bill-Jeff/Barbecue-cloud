package com.bbq.module.flashsale.consumer;

import com.bbq.module.flashsale.config.RabbitMQConfig;
import com.bbq.module.flashsale.entity.FlashSaleOrderMessage;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.entity.OrderItem;
import com.bbq.module.flashsale.mapper.FlashSaleMapper;
import com.bbq.module.flashsale.mapper.OrderItemMapper;
import com.bbq.module.flashsale.mapper.OrderMapper;
import com.bbq.module.flashsale.service.FlashSaleRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleRedisService flashSaleRedisService;

    @RabbitListener(queues = RabbitMQConfig.FLASH_SALE_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(FlashSaleOrderMessage msg) {
        log.info("收到秒杀订单消息: {}", msg);

        // 1. 扣减数据库库存
        int rows = flashSaleMapper.decrementStock(msg.getProductId(), msg.getQuantity());
        if (rows == 0) {
            // DB 库存不足，回滚 Redis 库存
            flashSaleRedisService.incrementStock(msg.getFlashSaleId(), msg.getQuantity());
            log.warn("数据库库存不足，已回滚 Redis 库存: flashSaleId={}", msg.getFlashSaleId());
            return;
        }

        // 2. 创建订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTableNo(msg.getTableNo());
        orderInfo.setTotalAmount(msg.getPrice().multiply(BigDecimal.valueOf(msg.getQuantity())));
        orderInfo.setStatus(0);
        orderMapper.insert(orderInfo);

        OrderItem item = new OrderItem();
        item.setOrderId(orderInfo.getId());
        item.setProductId(msg.getProductId());
        item.setProductName(msg.getProductName());
        item.setPrice(msg.getPrice());
        item.setQuantity(msg.getQuantity());
        orderItemMapper.insert(item);

        log.info("秒杀订单创建成功: orderId={}", orderInfo.getId());
    }
}
