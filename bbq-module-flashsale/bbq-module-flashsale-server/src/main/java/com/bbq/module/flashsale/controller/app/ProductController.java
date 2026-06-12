package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.Category;
import com.bbq.module.flashsale.entity.Product;
import com.bbq.module.flashsale.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flashsale/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.success(productService.listCategories());
    }

    @GetMapping("/products")
    public Result<Map<String, Object>> listProducts(@RequestParam(required = false) Long categoryId) {
        List<Product> products = productService.listProducts(categoryId);
        return Result.success(Map.of("list", products));
    }
}
