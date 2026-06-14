package com.bbq.module.flashsale.controller.app;

import com.bbq.module.flashsale.config.Result;
import com.bbq.module.system.api.auth.AuthApi;
import com.bbq.module.system.api.auth.dto.AuthLoginReqDTO;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.api.user.AdminUserApi;
import com.bbq.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.bbq.framework.common.pojo.CommonResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/flashsale/api/admin")
@RequiredArgsConstructor
public class LoginController {

    private final AdminUserApi userApi;
    private final AuthApi authApi;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespDTO> login(@RequestBody AuthLoginReqDTO body) {
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
    public CommonResult<AuthLoginRespDTO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return authApi.refresh(refreshToken);
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public CommonResult<List<AdminUserRespDTO>> listUsers() {
        List<Long> userIds = List.of(1L);
        return userApi.getUserList(userIds);
    }

//    @PostMapping("/users")
//    public Result<Void> saveUser(@RequestBody User user) {
//        userService.save(user);
//        return Result.success();
//    }

//    @DeleteMapping("/users/{id}")
//    public Result<Void> deleteUser(@PathVariable Long id) {
//        userApi.de(id);
//        return Result.success();
//    }
}
