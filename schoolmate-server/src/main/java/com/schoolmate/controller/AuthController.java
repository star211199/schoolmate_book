package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.auth.ForgotPasswordDTO;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
import com.schoolmate.dto.auth.ResetPasswordDTO;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.service.AuthService;
import com.schoolmate.vo.auth.LoginVO;
import com.schoolmate.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器：注册 / 登录 / 当前用户。
 *
 * @author Albot
 */
@Tag(name = "认证管理", description = "注册、登录、获取当前登录用户")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto), "注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto), "登录成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录凭证无效或已过期，请重新登录");
        }
        return Result.success(authService.getCurrentUser(userId));
    }

    @Operation(summary = "找回密码：发送邮箱验证码（10 分钟有效，需已绑定邮箱）")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.forgotPassword(dto);
        return Result.success(null, "验证码已发送，请查收邮箱");
    }

    @Operation(summary = "凭邮箱验证码重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPasswordByEmail(dto);
        return Result.success(null, "密码重置成功，请使用新密码登录");
    }

    @Operation(summary = "是否已配置邮件服务（前端据此决定是否展示邮箱找回入口）")
    @GetMapping("/mail-reset-enabled")
    public Result<Boolean> mailResetEnabled() {
        return Result.success(authService.mailResetEnabled());
    }
}
