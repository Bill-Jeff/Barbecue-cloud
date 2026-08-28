package com.bbq.module.flashsale.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbq.framework.common.pojo.PageResult;
import com.bbq.module.flashsale.entity.OrderInfo;
import com.bbq.module.flashsale.entity.OrderItem;
import com.bbq.module.flashsale.mapper.OrderItemMapper;
import com.bbq.module.flashsale.mapper.OrderMapper;
import com.bbq.module.flashsale.mq.producer.OrderTimeoutProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderInfoService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderTimeoutProducer orderTimeoutProducer;

    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderInfo orderInfo) {
        List<OrderItem> items = orderInfo.getItems();

        if (orderInfo.getStatus() == null) {
            orderInfo.setStatus(OrderInfo.STATUS_UNPAID);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        orderInfo.setTotalAmount(total);
        orderMapper.insert(orderInfo);
        for (OrderItem item : items) {
            item.setOrderId(orderInfo.getId());
        }
        orderItemMapper.insert(items);

        // 固定 TTL 队列在 30 分钟后将消息死信到订单过期队列。
        // 放在事务内发送：发送失败会回滚订单；事务回滚产生的孤儿消息会因查不到订单被安全忽略。
        orderTimeoutProducer.send(orderInfo.getId());
        return orderInfo.getId();
    }

    /**
     * 分页列出订单（含明细），按下单时间倒序，供管理后台滚动加载。
     *
     * @param offset 起始偏移量，从 0 开始
     * @param limit  每页条数
     */
    public PageResult<OrderInfo> listOrders(int offset, int limit) {
        // 1. 查询总记录数 (1次SQL)
        long total = orderMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
        );

        // 2. 查询当前页订单，按下单时间倒序 (1次SQL)
        List<OrderInfo> orders = orderMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .orderByDesc(OrderInfo::getCreateTime)
                        .last("LIMIT " + offset + ", " + limit)
        );

        if (CollUtil.isEmpty(orders)) {
            return new PageResult<OrderInfo>(orders, total);
        }

        // 3. 提取当前页订单ID
        List<Long> orderIds = orders.stream().map(OrderInfo::getId).collect(Collectors.toList());

        // 4. 批量查询当前页订单明细 (1次SQL)
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );

        // 5. 按 orderId 分组，提升后续匹配效率 (空间换时间)
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 6. 组装数据
        orders.forEach(order -> order.setItems(itemMap.get(order.getId())));

        return new PageResult<OrderInfo>(orders, total);
    }

    public void updateOrderStatus(OrderInfo orderInfo) {
        orderMapper.updateById(orderInfo);
    }

    public boolean updateStatusIfMatch(Long orderId, int expectedStatus, int targetStatus) {
        return orderMapper.updateStatusIfMatch(orderId, expectedStatus, targetStatus) == 1;
    }

    public OrderInfo getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    public OrderInfo getOrderByRequestId(String requestId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getRequestId, requestId));
    }
}
