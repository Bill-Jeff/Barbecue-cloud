package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.PageResult;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.service.OrderService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/flashsale/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 顾客下单：免登录（yudao @PermitAll） */
    @PostMapping
    @PermitAll
    public Result<Map<String, Object>> createOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.createOrder(orderInfo);
        return Result.success(Map.of("id", orderId));
    }

    /**
     * 分页列出顾客订单（含明细），供超级管理员在「点单记录」中滚动加载。
     *
     * @param offset 起始偏移量，从 0 开始
     * @param limit  每页条数，默认 10，最大 100
     */
    @GetMapping
    @PreAuthorize("@ss.hasRole('super_admin')")
    public Result<PageResult<OrderInfo>> listOrders(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return Result.success(orderService.listOrders(safeOffset, safeLimit));
    }
}
