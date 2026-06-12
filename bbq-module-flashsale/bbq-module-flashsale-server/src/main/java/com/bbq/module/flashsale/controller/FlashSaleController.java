package com.bbq.module.flashsale.controller;

import com.gitee.apanlh.annotation.viewresolver.RequestVo;
import com.gitee.apanlh.web.model.vo.RequestVO;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.FlashSale;
import com.bbq.module.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/api/flash-sales")
    public Result<List<FlashSale>> listActive() {
        return Result.success(flashSaleService.listActive());
    }

    @GetMapping("/api/admin/flash-sales")
    public Result<List<FlashSale>> listAll() {
        return Result.success(flashSaleService.listAll());
    }

    @PostMapping("/api/admin/flash-sales")
    public Result<Void> save(@RequestBody FlashSale flashSale) {
        flashSaleService.save(flashSale);
        return Result.success();
    }

    @DeleteMapping("/api/admin/flash-sales/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        flashSaleService.delete(id);
        return Result.success();
    }

    @PostMapping("/api/flash-sales/order")
    public Result<String> flashSaleOrder(@RequestVo RequestVO body) {
        Long flashSaleId = body.getLong("flashSaleId");
        String tableNo = body.getStr("tableNo");
        int quantity =body.getInt("quantity");
        flashSaleService.createFlashSaleOrder(flashSaleId, tableNo, quantity);
        return Result.success("抢购成功，正在处理订单");
    }
}
