package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
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
}
