package com.bbq.module.flashsale.controller;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Map<String, Object>> createOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.createOrder(orderInfo);
        return Result.success(Map.of("id", orderId));
    }
}
