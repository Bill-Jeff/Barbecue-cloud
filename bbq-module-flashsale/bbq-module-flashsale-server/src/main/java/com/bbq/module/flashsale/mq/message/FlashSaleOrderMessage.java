package com.bbq.module.flashsale.mq.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlashSaleOrderMessage {

    @Schema(description = "下单请求ID，由生产者预生成，用作消费幂等键")
    private String requestId;

    @Schema(description = "秒杀活动ID")
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
