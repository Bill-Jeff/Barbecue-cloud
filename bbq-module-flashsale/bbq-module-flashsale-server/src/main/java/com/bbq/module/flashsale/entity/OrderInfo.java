package com.bbq.module.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_info")
public class OrderInfo {
    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_PAYING = 2;
    public static final int STATUS_EXPIRED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String tableNo;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<OrderItem> items;
}
