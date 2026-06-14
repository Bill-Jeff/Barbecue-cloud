package com.bbq.module.system.api.auth;

import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.module.system.api.auth.dto.AuthLoginReqDTO;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 管理员用户")
public interface AuthApi {

    String PREFIX = ApiConstants.PREFIX + "/auth";

    @PostMapping(PREFIX + "/login")
    @Operation(summary = "通过用户 ID 查询用户")
    @Parameter(name = "id", description = "用户编号", example = "1", required = true)
    CommonResult<AuthLoginRespDTO> login(@RequestBody AuthLoginReqDTO respDTO);

    @PostMapping(PREFIX + "/refresh")
    @Operation(summary = "通过用户 ID 查询用户")
    CommonResult<AuthLoginRespDTO> refresh(@RequestParam("refreshToken") String refreshToken);

}
