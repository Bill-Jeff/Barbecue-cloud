package com.bbq.module.flashsale.mq.consumer;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.mq.message.UserUpdateBalanceMessage;
import com.bbq.module.flashsale.service.OrderInfoService;
import com.bbq.module.flashsale.service.PaymentMessageRetryService;
import com.bbq.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateBalanceConsumer {

    private final AdminUserApi adminUserApi;
    private final OrderInfoService orderInfoService;
    private final PaymentMessageRetryService paymentMessageRetryService;
    /**
     * 扣款用户编号：演示环境无用户体系，固定扣 admin 用户（id=1）的余额
     */
    private static final Long BALANCE_USER_ID = 1L;
    @RabbitListener(queues = "${bbqmq.rabbitmq.flash-sale.payment.queue:flash.sale.payment.queue}")
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(UserUpdateBalanceMessage msg) {
        if (msg == null || msg.getOrderId() == null) {
            throw new IllegalArgumentException("非法扣款消息: " + msg);
        }

        OrderInfo existing = orderInfoService.getOrder(msg.getOrderId());
        if (existing == null) {
            throw new IllegalStateException("扣款对应的订单不存在: orderId=" + msg.getOrderId());
        }
        if (Integer.valueOf(OrderInfo.STATUS_PAID).equals(existing.getStatus())) {
            log.info("秒杀扣款消息重复投递，订单已支付，跳过扣款: orderId={}", msg.getOrderId());
            paymentMessageRetryService.cancelPending(msg.getOrderId());
            return;
        }
        if (Integer.valueOf(OrderInfo.STATUS_EXPIRED).equals(existing.getStatus())) {
            log.info("订单已过期，跳过扣款: orderId={}", msg.getOrderId());
            paymentMessageRetryService.cancelPending(msg.getOrderId());
            return;
        }

        boolean claimed = orderInfoService.updateStatusIfMatch(
                msg.getOrderId(), OrderInfo.STATUS_UNPAID, OrderInfo.STATUS_PAYING);
        if (!claimed) {
            OrderInfo latest = orderInfoService.getOrder(msg.getOrderId());
            if (latest != null && (Integer.valueOf(OrderInfo.STATUS_PAID).equals(latest.getStatus())
                    || Integer.valueOf(OrderInfo.STATUS_PAYING).equals(latest.getStatus()))) {
                log.info("扣款消息已由其他消费者处理，跳过: orderId={}, status={}",
                        msg.getOrderId(), latest.getStatus());
                return;
            }
            throw new IllegalStateException("订单状态不允许扣款: orderId=" + msg.getOrderId());
        }

        BigDecimal amount = existing.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("订单金额非法: orderId=" + msg.getOrderId());
        }
        CommonResult<Boolean> result = adminUserApi.updateBalance(BALANCE_USER_ID, amount);
        if (result == null || result.isError()) {
            // 抛异常后 PAYING 状态回滚为 UNPAID，MQ 或 Redis 待发送集合会继续重试。
            throw new RuntimeException("秒杀扣款失败: " + (result == null ? "无响应" : result.getMsg()));
        }

        boolean paid = orderInfoService.updateStatusIfMatch(
                msg.getOrderId(), OrderInfo.STATUS_PAYING, OrderInfo.STATUS_PAID);
        if (!paid) {
            throw new IllegalStateException("更新订单支付状态失败: orderId=" + msg.getOrderId());
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentMessageRetryService.cancelPending(msg.getOrderId());
            }
        });
    }
}
