package com.bbq.framework.mq.rabbitmq.interfacefield;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component // 继承了 Spring 的 @Component，可以被扫描到
public @interface FlashSaleRoutingKey {
    String value(); // 比如 "flash.sale.order.create"
}