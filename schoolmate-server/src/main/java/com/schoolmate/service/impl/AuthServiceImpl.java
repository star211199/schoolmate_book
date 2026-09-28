package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.common.ResultCode;
import com.schoolmate.mapper.UserMapper;
import com.schoolmate.service.AuthService;
import com.schoolmate.service.UserProfileService;
import com.schoolmate.utils.JwtUtil;
import com.schoolmate.vo.auth.LoginVO;
import com.schoolmate.vo.user.UserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 认证服务实现。
 *
 * @author Albot
 */
@Slf4j
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    /** 用户角色：普通用户 / 管理员 */
    private static final String ROLE_USER = "USER";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_DISABLED = "DISABLED";

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserProfileService userProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        // 1. 账号唯一性校验
        Long count = this.lambdaQuery().eq(User::getUsername, dto.getUsername()).count();
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该账号已被注册");
        }

        // 2. 构造用户并加密密码
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(ROLE_USER);
        user.setStatus(STATUS_NORMAL);
        this.save(user);

        // 3. 初始化个人主页资料（保证 1:1 关系存在）
        userProfileService.initProfile(user.getId());

        // 4. 若携带邀请码，注册后自动加入班级
        if (StringUtils.hasText(dto.getInviteCode())) {
            // 延迟到 ClassService 处理，此处仅记录日志，避免认证模块反向依赖业务模块
            log.info("用户 {} 注册时携带邀请码 {}（加入班级由前端引导完成）", user.getUsername(), dto.getInviteCode());
        }

        return convertToVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号或密码错误");
        }
        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
            .token(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .role(user.getRole())
            .build();
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return convertToVO(user);
    }

    /**
     * 实体转 VO，屏蔽敏感字段并做手机号脱敏。
     */
    private UserVO convertToVO(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(desensitizePhone(user.getPhone()));
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 手机号脱敏：保留前 3 位与后 4 位。
     */
    private String desensitizePhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
