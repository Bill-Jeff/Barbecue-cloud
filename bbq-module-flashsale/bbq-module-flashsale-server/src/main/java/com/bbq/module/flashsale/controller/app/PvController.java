package com.bbq.module.flashsale.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.Product;
import com.bbq.module.flashsale.mapper.ProductMapper;
import com.bbq.module.flashsale.utils.UvPvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flashsale/api")
@RequiredArgsConstructor
public class PvController {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;
    private final UvPvUtil uvPvUtil;

    @PostMapping("/pv/{productId}")
    public Result<Void> addPv(@PathVariable Long productId) {
        uvPvUtil.add(String.valueOf(productId));
        return Result.success(null);
    }

    @GetMapping("/hot-products")
    public Result<List<Map<String, Object>>> getHotProducts(@RequestParam(defaultValue = "10") int limit) {
        String today = LocalDate.now().toString();
        Set<String> keys = stringRedisTemplate.keys("uv_pv:*_" + today);
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
            return Result.success(Collections.emptyList());
        }
        List<Long> ids = tops.stream().map(Long::valueOf).collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, ids));
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
        return Result.success(result);
    }
}
