package com.bbq.module.flashsale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.entity.OrderItem;
import com.bbq.module.flashsale.mapper.OrderItemMapper;
import com.bbq.module.flashsale.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Transactional
    public Long createOrder(OrderInfo orderInfo) {
        List<OrderItem> items = orderInfo.getItems();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        orderInfo.setTotalAmount(total);
        orderInfo.setStatus(0);
        orderMapper.insert(orderInfo);

        for (OrderItem item : items) {
            item.setOrderId(orderInfo.getId());
            orderItemMapper.insert(item);
        }
        return orderInfo.getId();
    }

    /**
     * 列出全部订单（含明细），按下单时间倒序，供管理后台查询顾客的点单记录。
     */
    public List<OrderInfo> listAllOrders() {
        List<OrderInfo> orders = orderMapper.selectList(
            new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getCreateTime)
        );
        for (OrderInfo order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            order.setItems(items);
        }
        return orders;
    }
}
