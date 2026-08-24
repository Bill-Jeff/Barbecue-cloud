package com.bbq.module.flashsale.controller.app;

import com.bbq.framework.common.biz.system.permission.PermissionCommonApi;
import com.bbq.module.flashsale.config.Result;
import com.bbq.module.system.api.auth.AuthApi;
import com.bbq.module.system.api.auth.dto.AuthLoginReqDTO;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.api.user.AdminUserApi;
import com.bbq.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bbq.framework.common.pojo.CommonResult;

import java.util.*;

@RestController
@RequestMapping("/flashsale/api/admin")
@RequiredArgsConstructor
public class LoginController {

    private final AdminUserApi userApi;
    private final AuthApi authApi;
    private final PermissionCommonApi permissionApi;

    @PostMapping("/login")
    @PermitAll
    public CommonResult<Map<String, Object>> login(@RequestBody AuthLoginReqDTO body) {
        CommonResult<AuthLoginRespDTO> result = authApi.login(body);
        AuthLoginRespDTO auth = result.getData();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", auth.getUserId());
        // 采用 yudao 标准的 OAuth2 token（由系统模块签发，网关透传 login-user 头校验）
        map.put("accessToken", auth.getAccessToken());
        map.put("refreshToken", auth.getRefreshToken());
        map.put("expiresTime", auth.getExpiresTime());

        try {
            AdminUserRespDTO user = userApi.getUser(auth.getUserId()).getData();
            map.put("nickname", user != null ? user.getNickname() : "用户");
            Boolean isAdmin = permissionApi.hasAnyRoles(auth.getUserId(), "super_admin").getData();
            map.put("role", Boolean.TRUE.equals(isAdmin) ? "admin" : "user");
        } catch (Exception e) {
            map.put("nickname", "用户");
            map.put("role", "user");
        }

        return CommonResult.success(map);
    }

    @GetMapping("/check")
    public Result<Void> checkAuth() {
        return Result.success();
    }

    /**
     * 刷新 accessToken（yudao 标准：调用系统模块的刷新接口）
     *
     * 注意：前端通过 JSON body 传 refreshToken，因此这里用 @RequestBody 接收。
     */
    @PostMapping("/refresh")
    @PermitAll
    public CommonResult<AuthLoginRespDTO> refresh(@RequestBody Map<String, String> body) {
        return authApi.refresh(body.get("refreshToken"));
    }

    // ========== 用户管理 ==========

    /** 用户列表：仅超级管理员 */
    @GetMapping("/users")
    @PreAuthorize("@ss.hasRole('super_admin')")
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
