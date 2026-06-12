package com.bbq.module.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("order_info")
public class OrderInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableNo;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<OrderItem> items;
}
