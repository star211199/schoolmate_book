package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.dto.auth.ForgotPasswordDTO;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
import com.schoolmate.dto.auth.ResetPasswordDTO;
import com.schoolmate.entity.User;
import com.schoolmate.vo.auth.LoginVO;
import com.schoolmate.vo.user.UserVO;

/**
 * 认证服务接口。
 *
 * @author Albot
 */
public interface AuthService extends IService<User> {

    /**
     * 用户注册。
     *
     * @param dto 注册入参
     * @return 注册成功的用户信息
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录。
     *
     * @param dto 登录入参
     * @return Token 与用户基本信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 找回密码：校验账号与邮箱匹配后，发送 6 位验证码（10 分钟有效）。
     *
     * <p>邮件服务未配置（无 spring.mail.host）时抛出明确提示，引导走管理员重置。
     */
    void forgotPassword(ForgotPasswordDTO dto);

    /**
     * 凭邮箱验证码重置密码。
     */
    void resetPasswordByEmail(ResetPasswordDTO dto);

    /**
     * 是否配置了邮件服务（决定登录页是否展示「邮箱找回」入口）。
     *
     * @return true=已配置 SMTP，可用邮箱验证码找回
     */
    boolean mailResetEnabled();
}
