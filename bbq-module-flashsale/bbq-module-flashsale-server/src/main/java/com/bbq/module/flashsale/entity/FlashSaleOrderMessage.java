package com.bbq.module.flashsale.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlashSaleOrderMessage {
    private Long flashSaleId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String tableNo;
    private int quantity;
}
