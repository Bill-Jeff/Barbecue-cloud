package com.bbq.module.system.api.auth;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.framework.common.util.object.BeanUtils;
import com.bbq.module.system.api.auth.dto.AuthLoginReqDTO;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.controller.admin.auth.vo.*;
import com.bbq.module.system.service.auth.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;


/**
 * Auth API 实现类，提供 RESTful API 给 Feign 调用
 *
 * @author 芋道源码
 */
@RestController
@Validated
@Slf4j
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final AdminAuthService adminAuthService;
    @Override
    public CommonResult<AuthLoginRespDTO> login(AuthLoginReqDTO respDTO) {
        AuthLoginReqVO bean = BeanUtils.toBean(respDTO, AuthLoginReqVO.class);
        AuthLoginRespVO login = adminAuthService.login(bean);
        return CommonResult.success(BeanUtils.toBean(login, AuthLoginRespDTO.class));
    }

    @Override
    public CommonResult<AuthLoginRespDTO> refresh(String refreshToken) {
        return CommonResult.success(BeanUtils.toBean(adminAuthService.refreshToken(refreshToken), AuthLoginRespDTO.class));
    }
}