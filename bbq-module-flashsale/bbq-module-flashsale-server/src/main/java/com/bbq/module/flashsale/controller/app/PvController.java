package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.service.PvService;
import com.bbq.module.flashsale.utils.UvPvUtil;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 浏览计数 / 热榜：免登录 */
@RestController
@RequestMapping("/flashsale/api")
@RequiredArgsConstructor
@PermitAll
public class PvController {
    private final PvService pvService;
    private final UvPvUtil uvPvUtil;

    @PostMapping("/pv/{productId}")
    public Result<Void> addPv(@PathVariable Long productId) {
        uvPvUtil.add(String.valueOf(productId));
        return Result.success(null);
    }

    @GetMapping("/hot-products")
    public Result<List<Map<String, Object>>> getHotProducts(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(pvService.getHotProducts(limit));
    }
}
