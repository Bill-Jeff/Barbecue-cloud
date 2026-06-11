package com.bbq.module.system.framework.rpc.config;

import com.bbq.module.infra.api.config.ConfigApi;
import com.bbq.module.infra.api.file.FileApi;
import com.bbq.module.infra.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "systemRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, WebSocketSenderApi.class, ConfigApi.class})
public class RpcConfiguration {
}
