package com.bbq.module.flashsale.mq.consumer;

import com.bbq.module.flashsale.mq.message.FlashSaleOrderMessage;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.entity.OrderItem;
import com.bbq.module.flashsale.mapper.FlashSaleMapper;
import com.bbq.module.flashsale.service.FlashSaleRedisService;
import com.bbq.module.flashsale.service.OrderInfoService;
import com.bbq.module.flashsale.service.PaymentMessageRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

/**
 * 秒杀订单消费者：异步执行「减库存 → 创建订单」，提交后触发扣款消息。
 * <p>
 * 一致性说明：
 * - DB 库存与订单在同一个本地事务内，异常时一起回滚；Redis 只在消息最终进入
 * 死信队列后补偿，避免每次 MQ 重试都重复增加库存。
 * - 订单创建后先把 orderId 写入 Redis 待发送集合，事务提交后立即发送扣款消息；
 * 发送失败或提交后进程退出时，由定时任务继续补发。
 * - Redis 库存已在生产者端（FlashSaleService.createFlashSaleOrder）原子扣减，
 * 此处仅在「DB 库存不足」时回补；其余失败路径依赖 MQ 重试/死信做补偿。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderConsumer {

    private final OrderInfoService orderInfoService;
    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleRedisService flashSaleRedisService;
    private final PaymentMessageRetryService paymentMessageRetryService;

    @RabbitListener(queues = "${bbqmq.rabbitmq.flash-sale.order.queue:flash.sale.order.queue}")
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(FlashSaleOrderMessage msg) {
        validateMessage(msg);
        log.info("收到秒杀订单消息: {}", msg);

        // requestId 唯一索引提供并发场景下的最终幂等保护，订单主键仍由 MySQL 自增生成。
        if (orderInfoService.getOrderByRequestId(msg.getRequestId()) != null) {
            log.info("秒杀订单消息重复投递，跳过处理: requestId={}", msg.getRequestId());
            return;
        }

        // 1. 扣减数据库库存（原子 UPDATE ... WHERE count >= ?，防超卖）
        int rows = flashSaleMapper.decrementStock(msg.getFlashSaleId(), msg.getProductId(), msg.getQuantity());
        if (rows == 0) {
            // DB 库存不足是确定性业务失败，不应重试；释放预占的 Redis 库存。
            flashSaleRedisService.incrementStock(msg.getFlashSaleId(), msg.getQuantity());
            log.warn("数据库库存不足，已回滚 Redis 库存: flashSaleId={}", msg.getFlashSaleId());
            return;
        }
        // 2. 创建订单（初始状态：待支付）
        OrderInfo orderInfo = OrderInfo.builder()
                .requestId(msg.getRequestId())
                .tableNo(msg.getTableNo())
                .totalAmount(msg.getPrice().multiply(BigDecimal.valueOf(msg.getQuantity())))
                .status(OrderInfo.STATUS_UNPAID)
                .build();

        OrderItem item = OrderItem.builder()
                .productId(msg.getProductId())
                .productName(msg.getProductName())
                .price(msg.getPrice())
                .quantity(msg.getQuantity())
                .build();

        orderInfo.setItems(List.of(item));
        orderInfoService.createOrder(orderInfo);

        // 3. 提交前登记待发送记录，提交后立即发送；发送失败由定时任务补发。
        paymentMessageRetryService.registerPending(orderInfo.getId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentMessageRetryService.sendNow(orderInfo.getId());
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    paymentMessageRetryService.cancelPending(orderInfo.getId());
                }
            }
        });
        log.info("秒杀订单创建成功，事务提交后发送扣款请求: orderId={}", orderInfo.getId());
    }

    private void validateMessage(FlashSaleOrderMessage msg) {
        if (msg == null || msg.getRequestId() == null || msg.getRequestId().isBlank()
                || msg.getRequestId().length() > 64
                || msg.getFlashSaleId() == null
                || msg.getProductId() == null || msg.getQuantity() <= 0
                || msg.getPrice() == null || msg.getPrice().signum() <= 0
                || msg.getProductName() == null || msg.getProductName().isBlank()
                || msg.getTableNo() == null || msg.getTableNo().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("非法秒杀订单消息: " + msg);
        }
    }
}
