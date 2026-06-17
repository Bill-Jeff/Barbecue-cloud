package com.bbq.module.flashsale.service;

import com.bbq.module.flashsale.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.bbq.module.flashsale.entity.Product;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PvService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;

    public List<Map<String,Object>> getHotProducts(int limit) {
        Set<String> keys = stringRedisTemplate.keys("uv_pv:*");
        Map<String, Long> pvMap = new HashMap<>(10);
        if (keys != null) {
            for (String key : keys) {
                String val = stringRedisTemplate.opsForValue().get(key);
                if (val != null) {
                    String pid = key.substring("uv_pv:".length(), key.lastIndexOf("_"));
                    try {
                        pvMap.merge(pid, Long.parseLong(val), Long::sum);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        List<String> tops = pvMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        if (tops.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = tops.stream().map(Long::valueOf).collect(Collectors.toList());
        List<Product> products = productMapper.getList(ids);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String pid : tops) {
            Product p = productMap.get(Long.valueOf(pid));
            if (p == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("price", p.getPrice());
            item.put("image", p.getImage());
            item.put("pv", pvMap.get(pid));
            result.add(item);
        }
        return result;
    }
}
