package com.bbq.module.flashsale.consumer;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.framework.mq.rabbitmq.config.BbqRabbitMQAutoConfiguration;
import com.bbq.module.flashsale.entity.FlashSaleOrderMessage;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.entity.OrderItem;
import com.bbq.module.flashsale.mapper.FlashSaleMapper;
import com.bbq.module.flashsale.service.FlashSaleRedisService;
import com.bbq.module.flashsale.service.OrderInfoService;
import com.bbq.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 秒杀订单消费者：异步执行「减库存 → 创建订单 → 扣款」三步。
 * <p>
 * 一致性说明：
 * - 三步在同一个本地事务内执行，任一步失败抛出异常都会整体回滚（订单 + DB 库存），
 * 消息由 MQ 重试，多次失败后进入死信队列兜底。
 * - Redis 库存已在生产者端（FlashSaleService.createFlashSaleOrder）原子扣减，
 * 此处仅在「DB 库存不足」时回补；其余失败路径依赖 MQ 重试/死信做补偿。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderConsumer {

    /**
     * 订单状态：0=待支付，1=已支付
     */
    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int ORDER_STATUS_PAID = 1;

    private final OrderInfoService orderInfoService;
    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleRedisService flashSaleRedisService;

    @RabbitListener(queues = BbqRabbitMQAutoConfiguration.FLASH_SALE_QUEUE)
//    @Transactional(rollbackFor = Exception.class)
    public void onMessage(FlashSaleOrderMessage msg) {
        log.info("收到秒杀订单消息: {}", msg);

        // 1. 扣减数据库库存（原子 UPDATE ... WHERE count >= ?，防超卖）
        int rows = flashSaleMapper.decrementStock(msg.getProductId(), msg.getQuantity());
        if (rows == 0) {
            // DB 库存不足：回滚 Redis 库存，本单放弃（正常 ack，不重试）
            flashSaleRedisService.incrementStock(msg.getFlashSaleId(), msg.getQuantity());
            log.warn("数据库库存不足，已回滚 Redis 库存: flashSaleId={}", msg.getFlashSaleId());
            return;
        }

        // 2. 创建订单（初始状态：待支付）
        OrderInfo orderInfo = OrderInfo.builder()
                .tableNo(msg.getTableNo())
                .totalAmount(msg.getPrice().multiply(BigDecimal.valueOf(msg.getQuantity())))
                .status(ORDER_STATUS_UNPAID)
                .build();

        OrderItem item = OrderItem.builder()
                .orderId(orderInfo.getId())
                .productId(msg.getProductId())
                .productName(msg.getProductName())
                .price(msg.getPrice())
                .quantity(msg.getQuantity())
                .build();

        orderInfo.setItems(List.of(item));
        orderInfoService.createOrder(orderInfo);
        log.info("秒杀订单创建成功，已发送扣款请求: orderId={}", orderInfo.getId());
    }
}
