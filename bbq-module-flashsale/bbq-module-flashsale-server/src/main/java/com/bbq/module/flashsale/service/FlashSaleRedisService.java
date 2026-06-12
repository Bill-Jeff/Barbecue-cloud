package com.bbq.module.flashsale.service;

import com.gitee.apanlh.util.base.MapUtils;
import com.bbq.module.flashsale.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * FlashSaleRedisService 是一个处理秒杀活动 Redis 操作的服务类
 * 使用 Redis 作为存储，实现秒杀活动的库存管理和已售数量统计
 */
@Service
@RequiredArgsConstructor
public class FlashSaleRedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 初始化库存到 Redis，在创建/更新秒杀活动时调用
     *
     * @param flashSaleId 秒杀活动ID
     * @param count       初始库存数量
     */
    public void initStock(Long flashSaleId, int count) {
        // 设置库存数量到 Redis
        RedisConfig.STOCK_KEY_PREFIX.set(String.valueOf(flashSaleId), count);
        // 设置已售数量为 0 到 Redis
        RedisConfig.SOLD_KEY_PREFIX.set(String.valueOf(flashSaleId), 0);
    }

    /**
     * 先扣再检查，扣成负数则回滚
     */
    public boolean decrementStock(Long flashSaleId, int quantity) {
        String key = String.valueOf(flashSaleId);
        Long remaining = RedisConfig.STOCK_KEY_PREFIX.decrement(key, quantity);
        if (remaining != null && remaining >= 0) {
            RedisConfig.SOLD_KEY_PREFIX.increment(key, quantity);
            return true;
        }
        // 库存不足，回滚
        RedisConfig.STOCK_KEY_PREFIX.increment(key, quantity);
        return false;
    }

    /**
     * 回滚库存：库存 + quantity，已售 - quantity
     */
    public void incrementStock(Long flashSaleId, int quantity) {
        String id = String.valueOf(flashSaleId);
        RedisConfig.STOCK_KEY_PREFIX.increment(id, quantity);
        RedisConfig.SOLD_KEY_PREFIX.increment( id, quantity);
    }

    /**
     * 查询剩余库存
     *
     * @param flashSaleId 秒杀活动ID
     * @return 剩余库存数量，如果不存在则返回0
     */
    public int getStock(Long flashSaleId) {
        String val = RedisConfig.STOCK_KEY_PREFIX.get(String.valueOf(flashSaleId), String.class);
        // 从 Redis 获取库存值
        return val == null ? 0 : Integer.parseInt(val);
        // 如果值不存在则返回0，否则转换为整数返回
    }

    /**
     * 缓存秒杀活动信息到 Redis Hash，下单时不需要查 DB
     */
    public void cacheFlashSaleInfo(Long flashSaleId, Long productId, Integer price, String productName) {
        Map<String, Object> map = MapUtils.newHashMap( item -> {
            item.put("productId", String.valueOf(productId));
            item.put("price", String.valueOf(price));
            item.put("productName", productName);
        });
//        RedisConfig.INFO_KEY_PREFIX.set(String.valueOf(flashSaleId), map);
        redisTemplate.opsForHash().putAll(RedisConfig.INFO_KEY + ":" +flashSaleId, map);
    }

    /**
     * 从 Redis 获取秒杀活动信息
     * @return Map(productId, price, productName)，不存在时返回 null
     */
    public Map<Object,Object> getFlashSaleInfo(Long flashSaleId) {
        String key = String.valueOf(flashSaleId);
//        Map<Object, Object> keys = RedisConfig.INFO_KEY_PREFIX.getKeys(key);
        return redisTemplate.opsForHash().entries(RedisConfig.INFO_KEY + ":"+ key);
    }

    /**
     * 删除秒杀活动 Redis 缓存
     */
    public void deleteCache(Long flashSaleId) {
        String id = String.valueOf(flashSaleId);
        RedisConfig.STOCK_KEY_PREFIX.delKey(id);
        RedisConfig.SOLD_KEY_PREFIX.delKey(id);
        RedisConfig.INFO_KEY_PREFIX.delKey(id);
    }
}
