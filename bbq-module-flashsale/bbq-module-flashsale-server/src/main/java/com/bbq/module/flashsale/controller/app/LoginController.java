package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.BusinessException;
import com.bbq.module.flashsale.config.JwtUtil;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.flashsale.config.ResultCode;
import com.bbq.module.system.api.auth.AuthApi;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.bbq.framework.common.pojo.CommonResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/flashsale/api/admin")
@RequiredArgsConstructor
public class LoginController {

    private final AdminUserApi userService;
    private final AuthApi authApi;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespDTO> login(@RequestBody AuthLoginRespDTO body) {
        return authApi.login(body);
    }

    @GetMapping("/check")
    public Result<Void> checkAuth() {
        return Result.success();
    }

    /**
     * 刷新 accessToken
     */
    @PostMapping("/refresh")
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !JwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String username = JwtUtil.parseRefreshToken(refreshToken).getSubject();
        Map<String, String> data = new HashMap<>();
        data.put("token", JwtUtil.generateToken(username));
        return Result.success(data);
    }

    // ========== 用户管理 ==========

//    @GetMapping("/users")
//    public Result<List<User>> listUsers() {
//        return Result.success(userService.listAll());
//    }
//
//    @PostMapping("/users")
//    public Result<Void> saveUser(@RequestBody User user) {
//        userService.save(user);
//        return Result.success();
//    }
//
//    @DeleteMapping("/users/{id}")
//    public Result<Void> deleteUser(@PathVariable Long id) {
//        userService.delete(id);
//        return Result.success();
//    }
}
