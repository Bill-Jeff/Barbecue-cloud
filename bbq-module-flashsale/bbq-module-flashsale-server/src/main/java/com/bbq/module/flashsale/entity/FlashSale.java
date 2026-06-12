package com.bbq.module.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("flash_sale")
public class FlashSale {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private BigDecimal price;
    private Integer count;
    private LocalDateTime saleTime;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String productImage;
    @TableField(exist = false)
    private BigDecimal originalPrice;
}
