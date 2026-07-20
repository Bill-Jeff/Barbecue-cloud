package com.bbq.module.crm.framework.rpc.config;

import com.bbq.module.system.api.auth.AuthApi;
import com.bbq.module.system.api.user.AdminUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "CrmRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AdminUserApi.class, AuthApi.class})
public class RpcConfiguration {
}
