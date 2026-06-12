//package com.bbq.module.flashsale.controller;
//
//import com.bbq.module.flashsale.config.BusinessException;
//import com.bbq.module.flashsale.config.JwtUtil;
//import com.bbq.module.flashsale.config.Result;
//import com.bbq.module.flashsale.config.ResultCode;
//import com.bbq.module.flashsale.entity.User;
//import com.bbq.module.flashsale.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin")
//@RequiredArgsConstructor
//public class LoginController {
//
//    private final UserService userService;
//
//    @PostMapping("/login")
//    public Result<Map<String, String>> login(@RequestBody Map<String, String> body) {
//        String username = body.get("username");
//        String password = body.get("password");
//
//        User user = userService.findByUsername(username);
//        if (user != null && user.getStatus() == 1 && userService.checkPassword(password, user.getPassword())) {
//            Map<String, String> data = new HashMap<>();
//            data.put("token", JwtUtil.generateToken(username));
//            data.put("refreshToken", JwtUtil.generateRefreshToken(username));
//            data.put("nickname", user.getNickname());
//            data.put("role", user.getRole());
//            return Result.success(data);
//        }
//        throw new BusinessException(ResultCode.LOGIN_FAILED);
//    }
//
//    @GetMapping("/check")
//    public Result<Void> checkAuth() {
//        return Result.success();
//    }
//
//    /**
//     * 刷新 accessToken
//     */
//    @PostMapping("/refresh")
//    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> body) {
//        String refreshToken = body.get("refreshToken");
//        if (refreshToken == null || !JwtUtil.validateRefreshToken(refreshToken)) {
//            throw new BusinessException(ResultCode.UNAUTHORIZED);
//        }
//        String username = JwtUtil.parseRefreshToken(refreshToken).getSubject();
//        Map<String, String> data = new HashMap<>();
//        data.put("token", JwtUtil.generateToken(username));
//        return Result.success(data);
//    }
//
//    // ========== 用户管理 ==========
//
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
//}
