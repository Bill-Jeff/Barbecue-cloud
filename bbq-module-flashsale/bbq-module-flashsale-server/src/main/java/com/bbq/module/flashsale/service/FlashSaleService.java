package com.bbq.module.flashsale.service;

import com.bbq.module.flashsale.config.BusinessException;
import com.bbq.module.flashsale.config.ResultCode;
import com.bbq.module.flashsale.entity.FlashSale;
import com.bbq.module.flashsale.entity.Product;
import com.bbq.module.flashsale.mapper.FlashSaleMapper;
import com.bbq.module.flashsale.mapper.ProductMapper;
import com.bbq.module.flashsale.mq.message.FlashSaleOrderMessage;
import com.bbq.module.flashsale.mq.producer.FlashSaleOrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleMapper flashSaleMapper;
    private final ProductMapper productMapper;
    private final FlashSaleRedisService flashSaleRedisService;
    private final FlashSaleOrderProducer flashSaleOrderProducer;

    public List<FlashSale> listActive() {
        return flashSaleMapper.listActive();
    }

    public List<FlashSale> listAll() {
        return flashSaleMapper.listAll();
    }

    public void save(FlashSale flashSale) {
        if (flashSale.getId() == null) {
            flashSaleMapper.insert(flashSale);
        } else {
            flashSaleMapper.updateById(flashSale);
        }
        // 同步库存 + 活动信息到 Redis
        flashSaleRedisService.initStock(flashSale.getId(), flashSale.getCount(),flashSale.getSaleTime());
        Product product = productMapper.selectById(flashSale.getProductId());
        String productName = product != null ? product.getName() : "秒杀商品";
        flashSaleRedisService.cacheFlashSaleInfo(flashSale.getId(), flashSale.getProductId(),
                flashSale.getPrice(), productName);
    }

    public void delete(Long id) {
        flashSaleMapper.deleteById(id);
        // 清理 Redis 缓存
        flashSaleRedisService.deleteCache(id);
    }

    /**
     * 秒杀下单：Redis 原子扣减 → MQ 异步创建订单
     * 10 万并发全走 Redis，不碰数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public void createFlashSaleOrder(Long flashSaleId, String tableNo, int quantity) {
        if (flashSaleId == null || tableNo == null || tableNo.isBlank() || quantity <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "秒杀活动、桌号和购买数量不能为空");
        }
        // 1. 从 Redis 获取活动信息，不查 DB
        Map<Object,Object> info = flashSaleRedisService.getFlashSaleInfo(flashSaleId);
        if (info == null || info.isEmpty()) {
            throw new BusinessException(ResultCode.FLASH_SALE_EXPIRED);
        }

        Long productId;
        BigDecimal price;
        String productName;
        try {
            productId = Long.valueOf(String.valueOf(info.get("productId")));
            price = new BigDecimal(String.valueOf(info.get("price")));
            productName = String.valueOf(info.get("productName"));
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀活动缓存数据异常");
        }
        if (price.signum() <= 0 || productName.isBlank() || "null".equals(productName)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀活动缓存数据异常");
        }

        // 2. Redis 原子扣减
        boolean success = flashSaleRedisService.decrementStock(flashSaleId, quantity);
        if (!success) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不足");
        }
        // 3. 发送 MQ 消息，异步写订单
        FlashSaleOrderMessage msg = new FlashSaleOrderMessage();
        msg.setRequestId(UUID.randomUUID().toString());
        msg.setFlashSaleId(flashSaleId);
        msg.setProductId(productId);
        msg.setProductName(productName);
        msg.setPrice(price);
        msg.setTableNo(tableNo);
        msg.setQuantity(quantity);
        try {
            flashSaleOrderProducer.sendOrder(msg);
        } catch (RuntimeException ex) {
            flashSaleRedisService.incrementStock(flashSaleId, quantity);
            log.error("秒杀订单消息发送失败，已回补 Redis 库存: requestId={}", msg.getRequestId(), ex);
            throw ex;
        }
    }
}
