package com.bbq.module.flashsale.consumer;

import com.bbq.framework.mq.rabbitmq.config.BbqRabbitMQAutoConfiguration;
import com.bbq.module.flashsale.config.RabbitMQConfig;
import com.bbq.module.flashsale.entity.FlashSaleOrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FlashSaleDlxConsumer {

    @RabbitListener(queues = BbqRabbitMQAutoConfiguration.DLX_QUEUE)
    public void handleDlxMessage(FlashSaleOrderMessage msg) {
        log.error("收到秒杀死信消息（处理失败）: {}", msg);
        // 1. 记录失败日志到数据库，供人工介入
        // 2. 发送告警通知（钉钉、邮件等）
        // 3. 不再重试，因为已多次失败
        // 可选：如果是暂时性失败（如网络抖动），可以延迟后重新发布到主队列
        // 但更推荐结合延迟队列插件实现重试，下文会说明
    }
}