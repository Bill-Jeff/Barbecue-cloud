package com.bbq.module.system.api.auth;

import cn.hutool.core.util.ObjectUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.bbq.framework.common.enums.CommonStatusEnum;
import com.bbq.framework.common.enums.UserTypeEnum;
import com.bbq.framework.common.pojo.CommonResult;
import com.bbq.framework.common.util.monitor.TracerUtils;
import com.bbq.framework.common.util.object.BeanUtils;
import com.bbq.framework.common.util.servlet.ServletUtils;
import com.bbq.framework.common.util.validation.ValidationUtils;
import com.bbq.framework.datapermission.core.annotation.DataPermission;
import com.bbq.module.system.api.auth.dto.AuthLoginRespDTO;
import com.bbq.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.bbq.module.system.api.sms.SmsCodeApi;
import com.bbq.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import com.bbq.module.system.api.social.dto.SocialUserBindReqDTO;
import com.bbq.module.system.api.social.dto.SocialUserRespDTO;
import com.bbq.module.system.controller.admin.auth.vo.*;
import com.bbq.module.system.convert.auth.AuthConvert;
import com.bbq.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.bbq.module.system.dal.dataobject.user.AdminUserDO;
import com.bbq.module.system.enums.logger.LoginLogTypeEnum;
import com.bbq.module.system.enums.logger.LoginResultEnum;
import com.bbq.module.system.enums.oauth2.OAuth2ClientConstants;
import com.bbq.module.system.enums.sms.SmsSceneEnum;
import com.bbq.module.system.service.auth.AdminAuthService;
import com.bbq.module.system.service.logger.LoginLogService;
import com.bbq.module.system.service.member.MemberService;
import com.bbq.module.system.service.oauth2.OAuth2TokenService;
import com.bbq.module.system.service.social.SocialUserService;
import com.bbq.module.system.service.user.AdminUserService;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.bbq.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.bbq.framework.common.util.servlet.ServletUtils.getClientIP;
import static com.bbq.module.system.enums.ErrorCodeConstants.*;

/**
 * Auth Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final AdminAuthService adminAuthService;
    @Override
    public CommonResult<AuthLoginRespDTO> login(AuthLoginRespDTO respDTO) {
        AuthLoginReqVO bean = BeanUtils.toBean(respDTO, AuthLoginReqVO.class);
        AuthLoginRespVO login = adminAuthService.login(bean);
        return CommonResult.success(BeanUtils.toBean(login, AuthLoginRespDTO.class));
    }
}