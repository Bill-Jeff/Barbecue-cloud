package com.bbq.module.flashsale.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlashSaleOrderMessage {

    @Schema(description = "订单ID")
    private Long flashSaleId;

    @Schema(description = "食品ID")
    private Long productId;

    @Schema(description = "食品名称")
    private String productName;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "桌号")
    private String tableNo;

    @Schema(description = "订单数量")
    private int quantity;
}
