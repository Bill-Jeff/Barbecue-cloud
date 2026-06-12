package com.bbq.module.flashsale.utils;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * UvPvUtil类是一个用于统计UV（独立访客）和PV（页面浏览量）的工具类
 * 使用ConcurrentHashMap和LongAdder实现高效的计数功能，并通过定时任务将结果保存到Redis
 * @author 13518
 */
@Component
@RequiredArgsConstructor
public class UvPvUtil {

    private final StringRedisTemplate stringRedisTemplate;

    private final ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    private static final String PREFIX = "uv_pv:";

    /**
     * 添加计数
     * @param pid 编号
     *
     */
    public void add(String pid) {
        String key = PREFIX + pid + "_" + LocalDate.now();
        // 使用computeIfAbsent方法，如果key不存在则创建新的LongAdder，然后增加计数
        map.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public Long get(String pid) {
        String key = pid + "_" + LocalDate.now();
        return stringRedisTemplate.opsForValue().size(key);
    }

    /**
     * 定时将计数结果保存到Redis中
     * 使用@Scheduled注解，每5秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    public void saveToRedis() {
        if(!running) {
            return;
        }
        // 遍历map中的所有计数器，将计数累加后重置，并保存到Redis
        map.forEach((key, count) -> {
            long num = count.sumThenReset(); // 获取计数并重置
            if (num > 0 ) { // 如果计数大于0，则保存到Redis
                stringRedisTemplate.opsForValue().increment(key, num);
            }
        });
    }

    /**
     * 服务关闭前最后 flush 一次，减少数据丢失
     * 使用@PreDestroy注解，在服务销毁前执行
     */
    @PreDestroy
    public void destroy() {
        running = false;
        saveToRedis();
    }
}
