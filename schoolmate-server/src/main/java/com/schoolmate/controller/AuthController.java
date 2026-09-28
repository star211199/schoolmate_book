package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
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
        return Result.success(authService.getCurrentUser(userId));
    }
}
