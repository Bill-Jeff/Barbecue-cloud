package com.bbq.module.flashsale.controller.app;

import com.gitee.apanlh.annotation.viewresolver.RequestVo;
import com.gitee.apanlh.web.model.vo.RequestVO;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.FlashSale;
import com.bbq.module.flashsale.service.FlashSaleService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flashsale/api")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    /** 查询进行中的秒杀活动：免登录 */
    @GetMapping("/flash-sales")
    @PermitAll
    public Result<List<FlashSale>> listActive() {
        return Result.success(flashSaleService.listActive());
    }

    /** 秒杀活动管理：仅超级管理员 */
    @GetMapping("/admin/flash-sales")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public Result<List<FlashSale>> listAll() {
        return Result.success(flashSaleService.listAll());
    }

    /** 秒杀活动管理：仅超级管理员 */
    @PostMapping("/admin/flash-sales")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public Result<Void> save(@RequestBody FlashSale flashSale) {
        flashSaleService.save(flashSale);
        return Result.success();
    }

    /** 秒杀活动管理：仅超级管理员 */
    @DeleteMapping("/admin/flash-sales/{id}")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public Result<Void> delete(@PathVariable Long id) {
        flashSaleService.delete(id);
        return Result.success();
    }

    /** 顾客抢购秒杀：免登录 */
    @PostMapping("/flash-sales/order")
    @PermitAll
    public Result<String> flashSaleOrder(@RequestVo RequestVO body) {
        Long flashSaleId = body.getLong("flashSaleId");
        String tableNo = body.getStr("tableNo");
        int quantity =body.getInt("quantity");
        flashSaleService.createFlashSaleOrder(flashSaleId, tableNo, quantity);
        return Result.success("抢购成功，正在处理订单");
    }
}
