package com.bbq.module.flashsale.config;

import com.gitee.apanlh.annotation.redis.RegisterRedisKey;
import com.gitee.apanlh.spring.data.redis.api.RedisHash;
import com.gitee.apanlh.spring.data.redis.api.RedisString;
import com.gitee.apanlh.spring.data.redis.builder.RedisKeyBuilder;

/**
 * Redis配置类，用于定义Redis键的前缀配置
 * 使用@RegisterRedisKey注解标记为Redis键的注册类
 */
@RegisterRedisKey
public class RedisConfig {

    public static final String STOCK_KEY = "flash:stock";
    public static final String SOLD_KEY = "flash:sold";
    public static final String INFO_KEY = "flash:info";

    /**
     * 定义商品库存的Redis键前缀，格式为"flash:stock:"
      */
    public static final RedisString STOCK_KEY_PREFIX = RedisKeyBuilder.builder().withKeys(STOCK_KEY).buildString();;

    /**
     * 定义已售商品的Redis键前缀，格式为"flash:sold:"
      */
    public static final RedisString SOLD_KEY_PREFIX = RedisKeyBuilder.builder().withKeys(SOLD_KEY).buildString();

    /**
     * 秒杀活动信息缓存，格式为 "flash:info:"
     */
    public static final RedisHash INFO_KEY_PREFIX = RedisKeyBuilder.builder().withKeys(INFO_KEY).buildHash();

}
