package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flashsale/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Map<String, Object>> createOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.createOrder(orderInfo);
        return Result.success(Map.of("id", orderId));
    }

    /**
     * 列出全部顾客订单（含明细），供超级管理员在「点单记录」中查看。
     */
    @GetMapping
    public Result<List<OrderInfo>> listOrders() {
        return Result.success(orderService.listAllOrders());
    }
}
