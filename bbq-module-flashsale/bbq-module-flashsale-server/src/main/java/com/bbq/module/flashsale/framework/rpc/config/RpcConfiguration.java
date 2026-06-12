package com.bbq.module.flashsale.framework.rpc.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "flashSaleRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients()
public class RpcConfiguration {
}
