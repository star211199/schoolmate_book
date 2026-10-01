package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.RandomUtil;
import com.schoolmate.dto.auth.ForgotPasswordDTO;
import com.schoolmate.dto.auth.LoginDTO;
import com.schoolmate.dto.auth.RegisterDTO;
import com.schoolmate.dto.auth.ResetPasswordDTO;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

    /* ==================== 找回密码 ==================== */

    /** 验证码有效期 10 分钟 */
    private static final long CODE_TTL_MILLIS = 10 * 60 * 1000L;
    /** 同一账号两次发码的最小间隔 60 秒（防刷） */
    private static final long CODE_RESEND_INTERVAL_MILLIS = 60 * 1000L;

    /**
     * 找回密码验证码：username → {code, 过期时刻, 上次发送时刻}。
     * 单节点内存存储，与在线状态同一设计取舍；集群化时需迁 Redis。
     */
    private final Map<String, CodeEntry> resetCodes = new ConcurrentHashMap<>();

    /** 未配置 spring.mail.host 时容器里没有 JavaMailSender，用 ObjectProvider 延迟判空 */
    @Resource
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private record CodeEntry(String code, long expireAt, long lastSentAt) {
    }

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

    @Override
    public void forgotPassword(ForgotPasswordDTO dto) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException(ResultCode.ERROR, "邮件服务未配置，请联系管理员重置密码");
        }
        User user = this.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        // 不区分「账号不存在」与「邮箱不匹配」，避免被用来探测有效账号
        if (user == null || !StringUtils.hasText(user.getEmail())
            || !user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号与邮箱不匹配");
        }

        CodeEntry existing = resetCodes.get(user.getUsername());
        if (existing != null
            && System.currentTimeMillis() - existing.lastSentAt() < CODE_RESEND_INTERVAL_MILLIS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码发送太频繁，请 1 分钟后再试");
        }

        String code = RandomUtil.randomNumbers(6);
        resetCodes.put(user.getUsername(),
            new CodeEntry(code, System.currentTimeMillis() + CODE_TTL_MILLIS, System.currentTimeMillis()));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("【大学同学录】找回密码验证码");
        message.setText("你正在找回登录密码，验证码：" + code + "，10 分钟内有效。\n"
            + "如果这不是你的操作，请忽略本邮件。");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            resetCodes.remove(user.getUsername());
            log.error("找回密码验证码发送失败：{}", e.getMessage());
            throw new BusinessException(ResultCode.ERROR, "验证码发送失败，请稍后再试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByEmail(ResetPasswordDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        if (user == null || !StringUtils.hasText(user.getEmail())
            || !user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账号与邮箱不匹配");
        }

        CodeEntry entry = resetCodes.get(user.getUsername());
        if (entry == null || System.currentTimeMillis() > entry.expireAt()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        if (!entry.code().equals(dto.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
        // 验证码一次性使用，重置成功后立即作废
        resetCodes.remove(user.getUsername());
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

    @Override
    public boolean mailResetEnabled() {
        return mailSenderProvider.getIfAvailable() != null;
    }
}
