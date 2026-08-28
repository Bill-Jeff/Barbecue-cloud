package com.bbq.framework.mq.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列配置类
 * <p>
 * 所有队列/交换机/路由键均通过配置项 {@code bbqmq.rabbitmq.flash-sale.*} 外部化，
 * 未配置时回退到默认值（与默认 yaml 保持一致）。
 * <p>
 * v2 说明：旧队列未配置死信参数，而 RabbitMQ 不允许在线修改已有队列属性，
 * 因此切换为「新队列名 + 死信参数」的声明方式，如需平滑迁移请换用新队列名。
 *
 * @author 芋道源码
 */
@AutoConfiguration
@Slf4j
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class BbqRabbitMQAutoConfiguration {

    // ---------- 秒杀主交换机（order / payment 共用）----------
    @Value("${bbqmq.rabbitmq.flash-sale.exchange:flash.sale.exchange}")
    private String flashSaleExchange;

    // ---------- 秒杀订单主队列（异步创建订单）----------
    @Value("${bbqmq.rabbitmq.flash-sale.order.queue:flash.sale.order.queue}")
    private String flashSaleOrderQueue;
    @Value("${bbqmq.rabbitmq.flash-sale.order.routing-key:flash.sale.order.create}")
    private String flashSaleOrderRoutingKey;

    // ---------- 秒杀扣款队列（事务提交后异步扣款）----------
    @Value("${bbqmq.rabbitmq.flash-sale.payment.queue:flash.sale.payment.queue}")
    private String flashSalePaymentQueue;
    @Value("${bbqmq.rabbitmq.flash-sale.payment.routing-key:flash.sale.payment.create}")
    private String flashSalePaymentRoutingKey;

    // ---------- 死信队列相关（Topic 模式）----------
    @Value("${bbqmq.rabbitmq.flash-sale.dlx.exchange:flash.sale.dlx.exchange}")
    private String flashSaleDlxExchange;
    @Value("${bbqmq.rabbitmq.flash-sale.dlx.queue:flash.sale.dlx.queue}")
    private String flashSaleDlxQueue;
    @Value("${bbqmq.rabbitmq.flash-sale.dlx.routing-key:flash.sale.dlx}")
    private String flashSaleDlxRoutingKey;

    // ---------- 订单超时队列：等待 30 分钟后死信到过期队列 ----------
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.delay-queue:flash.sale.order.timeout.delay.queue}")
    private String flashSaleTimeoutDelayQueue;
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.routing-key:flash.sale.order.timeout}")
    private String flashSaleTimeoutRoutingKey;
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.ttl:1800000}")
    private long flashSaleTimeoutTtl;
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.expired-exchange:flash.sale.order.expired.exchange}")
    private String flashSaleExpiredExchange;
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.expired-queue:flash.sale.order.expired.queue}")
    private String flashSaleExpiredQueue;
    @Value("${bbqmq.rabbitmq.flash-sale.timeout.expired-routing-key:flash.sale.order.expired}")
    private String flashSaleExpiredRoutingKey;

    @Bean
    public Queue flashSaleOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        // 指定死信交换机与死信路由键（消息重试耗尽后进入死信）
        args.put("x-dead-letter-exchange", flashSaleDlxExchange);
        args.put("x-dead-letter-routing-key", flashSaleDlxRoutingKey);
        return new Queue(flashSaleOrderQueue, true, false, false, args);
    }

    @Bean
    public Queue flashSalePaymentQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", flashSaleDlxExchange);
        args.put("x-dead-letter-routing-key", flashSaleDlxRoutingKey);
        return new Queue(flashSalePaymentQueue, true, false, false, args);
    }

    /**
     * 主交换机：order / payment 两条链路共用，通过不同路由键区分
     */
    @Bean
    public TopicExchange flashSaleExchange() {
        return new TopicExchange(flashSaleExchange, true, false);
    }

    @Bean
    public Binding flashSaleOrderBinding(Queue flashSaleOrderQueue, TopicExchange flashSaleExchange) {
        return BindingBuilder.bind(flashSaleOrderQueue)
                .to(flashSaleExchange)
                .with(flashSaleOrderRoutingKey);
    }

    @Bean
    public Binding flashSalePaymentBinding(Queue flashSalePaymentQueue, TopicExchange flashSaleExchange) {
        return BindingBuilder.bind(flashSalePaymentQueue)
                .to(flashSaleExchange)
                .with(flashSalePaymentRoutingKey);
    }

    // ---------- 死信交换机与死信队列（Topic 模式）----------
    @Bean
    public TopicExchange flashSaleDlxExchange() {
        return new TopicExchange(flashSaleDlxExchange, true, false);
    }

    @Bean
    public Queue flashSaleDlxQueue() {
        return new Queue(flashSaleDlxQueue, true);
    }

    @Bean
    public Binding flashSaleDlxBinding(Queue flashSaleDlxQueue, TopicExchange flashSaleDlxExchange) {
        return BindingBuilder.bind(flashSaleDlxQueue)
                .to(flashSaleDlxExchange)
                .with(flashSaleDlxRoutingKey);
    }

    @Bean
    public Queue flashSaleTimeoutDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", flashSaleTimeoutTtl);
        args.put("x-dead-letter-exchange", flashSaleExpiredExchange);
        args.put("x-dead-letter-routing-key", flashSaleExpiredRoutingKey);
        return new Queue(flashSaleTimeoutDelayQueue, true, false, false, args);
    }

    @Bean
    public Binding flashSaleTimeoutDelayBinding(
            @Qualifier("flashSaleTimeoutDelayQueue") Queue flashSaleTimeoutDelayQueue,
            @Qualifier("flashSaleExchange") TopicExchange flashSaleExchange) {
        return BindingBuilder.bind(flashSaleTimeoutDelayQueue)
                .to(flashSaleExchange)
                .with(flashSaleTimeoutRoutingKey);
    }

    @Bean
    public TopicExchange flashSaleExpiredExchange() {
        return new TopicExchange(flashSaleExpiredExchange, true, false);
    }

    @Bean
    public Queue flashSaleExpiredQueue() {
        return new Queue(flashSaleExpiredQueue, true);
    }

    @Bean
    public Binding flashSaleExpiredBinding(
            @Qualifier("flashSaleExpiredQueue") Queue flashSaleExpiredQueue,
            @Qualifier("flashSaleExpiredExchange") TopicExchange flashSaleExpiredExchange) {
        return BindingBuilder.bind(flashSaleExpiredQueue)
                .to(flashSaleExpiredExchange)
                .with(flashSaleExpiredRoutingKey);
    }

    /**
     * 自定义 RabbitTemplate：覆盖 Boot 默认模板，保证统一使用 Jackson 消息转换器。
     * 开启 mandatory + ReturnsCallback，无法路由（如交换机/绑定配置错误）的消息会记录错误日志，
     * 避免「消息静默丢失、订单已建却扣不到款」这类问题无法定位。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned ->
                log.error("MQ 消息无法路由（交换机/路由键不匹配），消息被退回: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return rabbitTemplate;
    }

    /**
     * 泛型、类型安全的消息发送器，支持任意消息体类型 T。
     */
    @Bean
    public MqSender mqSender(RabbitTemplate rabbitTemplate) {
        return new MqSender(rabbitTemplate);
    }

    /**
     * 泛型 MQ 发送器：<T> 提供编译期类型安全，调用方无需强制转换。
     */
    public static class MqSender {
        private final RabbitTemplate rabbitTemplate;

        public MqSender(RabbitTemplate rabbitTemplate) {
            this.rabbitTemplate = rabbitTemplate;
        }

        public <T> void send(String exchange, String routingKey, T payload) {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        }
    }

    /**
     * Jackson2JsonMessageConverter Bean：使用 jackson 序列化消息。
     * 默认构造器自带类型映射（__TypeId__ 头 + @RabbitListener 参数类型推断），无需额外定制。
     */
    @Bean
    public MessageConverter createMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
