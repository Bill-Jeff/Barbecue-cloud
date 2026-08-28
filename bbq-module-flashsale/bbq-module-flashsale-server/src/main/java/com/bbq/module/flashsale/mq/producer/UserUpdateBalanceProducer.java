package com.bbq.module.flashsale.mq.consumer;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.framework.mq.rabbitmq.config.BbqRabbitMQAutoConfiguration;
import com.bbq.module.flashsale.mq.message.FlashSaleOrderMessage;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.mq.message.UserUpdateBalanceMessage;
import com.bbq.module.flashsale.service.OrderInfoService;
import com.bbq.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateBalanceConsumer {

    private final AdminUserApi adminUserApi;
    private final OrderInfoService orderInfoService;
    /**
     * 扣款用户编号：演示环境无用户体系，固定扣 admin 用户（id=1）的余额
     */
    private static final Long BALANCE_USER_ID = 1L;
    /**
     * 订单状态：0=待支付，1=已支付
     */
    private static final int ORDER_STATUS_PAID = 1;

    @RabbitListener(queues = BbqRabbitMQAutoConfiguration.FLASH_SALE_QUEUE)
    public void onMessage(UserUpdateBalanceMessage msg) {
        // 3. 扣款：余额扣减，金额 = 单价 × 数量
        BigDecimal amount = msg.getPrice().multiply(BigDecimal.valueOf(msg.getQuantity()));
        CommonResult<Boolean> result = adminUserApi.updateBalance(BALANCE_USER_ID, amount);
        if (result == null || result.isError()) {
            // 扣款失败：抛异常 → 事务回滚（订单 + DB 库存）→ MQ 重试 → 死信队列兜底
            throw new RuntimeException("秒杀扣款失败: " + (result == null ? "无响应" : result.getMsg()));
        }

        // 4. 扣款成功 → 订单标记为已支付
        OrderInfo update = new OrderInfo();
        update.setId(msg.getOrderId());
        update.setStatus(ORDER_STATUS_PAID);
        orderInfoService.updateOrderStatus(update);
    }
}
