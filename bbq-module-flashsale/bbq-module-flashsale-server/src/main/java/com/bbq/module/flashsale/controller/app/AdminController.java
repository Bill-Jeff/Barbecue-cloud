package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.entity.Category;
import com.bbq.module.flashsale.entity.Product;
import com.bbq.module.flashsale.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/flashsale/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    // ========== 分类管理 ==========

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.success(productService.listCategories());
    }

    @PostMapping("/categories")
    public Result<Void> saveCategory(@RequestBody Category category) {
        productService.saveCategory(category);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return Result.success();
    }

    // ========== 菜品管理 ==========

    @GetMapping("/products")
    public Result<List<Product>> listAllProducts() {
        return Result.success(productService.listAllProducts());
    }

    @PostMapping("/products")
    public Result<Void> saveProduct(@RequestBody Product product) {
        productService.saveProduct(product);
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    // ========== 图片上传 ==========

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();

        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + ext;
        Path path = Paths.get(UPLOAD_DIR, fileName);
        Files.write(path, file.getBytes());

        return Result.success(Map.of("url", "/uploads/" + fileName));
    }
}
