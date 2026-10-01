package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.user.PasswordUpdateDTO;
import com.schoolmate.dto.user.ProfileUpdateDTO;
import com.schoolmate.dto.user.UserUpdateDTO;
import com.schoolmate.entity.User;
import com.schoolmate.entity.UserProfile;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.UserMapper;
import com.schoolmate.service.UserProfileService;
import com.schoolmate.service.UserService;
import com.schoolmate.utils.ConstellationUtil;
import com.schoolmate.vo.user.UserProfileVO;
import com.schoolmate.vo.user.UserVO;
import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 用户服务实现。
 *
 * @author Albot
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_DISABLED = "DISABLED";

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public UserVO getUserById(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        // 越权校验：仅本人或管理员可修改
        assertSelfOrAdmin(id);
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        this.updateById(user);
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long id, PasswordUpdateDTO dto) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        UserProfile profile = userProfileService.lambdaQuery()
            .eq(UserProfile::getUserId, userId)
            .one();
        if (profile == null) {
            // 兜底：资料缺失时自动初始化，保证接口幂等
            userProfileService.initProfile(userId);
            return new UserProfileVO();
        }
        return convertToProfileVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, ProfileUpdateDTO dto) {
        assertSelfOrAdmin(userId);
        UserProfile profile = new UserProfile();
        profile.setRealName(dto.getRealName());
        profile.setStudentNo(dto.getStudentNo());
        profile.setGender(dto.getGender());
        profile.setBirthday(dto.getBirthday());
        // 生日变更时自动重算星座
        profile.setConstellation(ConstellationUtil.of(dto.getBirthday()));
        profile.setMbti(dto.getMbti());
        profile.setHobbies(toJsonArray(dto.getHobbies()));
        profile.setSkills(toJsonArray(dto.getSkills()));
        profile.setGraduationMessage(dto.getGraduationMessage());
        profile.setSocialLinks(toJsonObject(dto.getSocialLinks()));
        profile.setCoverImage(dto.getCoverImage());
        profile.setHometown(dto.getHometown());
        profile.setCurrentCity(dto.getCurrentCity());
        profile.setContact(dto.getContact());
        profile.setMotto(dto.getMotto());
        profile.setEnrollmentYear(dto.getEnrollmentYear());
        UserProfile saved = userProfileService.saveOrUpdateProfile(userId, profile);
        return convertToProfileVO(saved);
    }

    @Override
    public PageResult<UserVO> pageUsers(Long pageNum, Long pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                .or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        if (!STATUS_NORMAL.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPasswordByAdmin(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 随机 8 位新密码；只在这里明文返回一次，库中仍只存 BCrypt 密文
        String newPassword = RandomUtil.randomString(8);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        return newPassword;
    }

    /**
     * 越权校验：仅本人或管理员可执行。
     */
    private void assertSelfOrAdmin(Long targetUserId) {
        Long currentUserId = UserContext.getUserId();
        boolean isSelf = Objects.equals(currentUserId, targetUserId);
        if (!isSelf && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改本人的信息");
        }
    }

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

    private UserProfileVO convertToProfileVO(UserProfile profile) {
        if (Objects.isNull(profile)) {
            return null;
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(profile.getUserId());
        vo.setRealName(profile.getRealName());
        vo.setStudentNo(profile.getStudentNo());
        vo.setGender(profile.getGender());
        vo.setBirthday(profile.getBirthday());
        vo.setConstellation(profile.getConstellation());
        vo.setMbti(profile.getMbti());
        vo.setHobbies(fromJsonArray(profile.getHobbies()));
        vo.setSkills(fromJsonArray(profile.getSkills()));
        vo.setGraduationMessage(profile.getGraduationMessage());
        vo.setSocialLinks(fromJsonObject(profile.getSocialLinks()));
        vo.setCoverImage(profile.getCoverImage());
        vo.setHometown(profile.getHometown());
        vo.setCurrentCity(profile.getCurrentCity());
        vo.setContact(profile.getContact());
        vo.setMotto(profile.getMotto());
        vo.setEnrollmentYear(profile.getEnrollmentYear());
        // 附带 user 表的头像与昵称
        User user = this.getById(profile.getUserId());
        if (Objects.nonNull(user)) {
            vo.setAvatar(user.getAvatar());
            vo.setNickname(user.getNickname());
        }
        return vo;
    }

    /** 标签列表转 JSON 数组字符串存储 */
    private String toJsonArray(java.util.List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "标签格式非法");
        }
    }

    /** JSON 数组字符串转标签列表 */
    private java.util.List<String> fromJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return java.util.Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return java.util.Collections.emptyList();
        }
    }

    /** 社交链接 Map 转 JSON 字符串存储 */
    private String toJsonObject(java.util.Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "社交链接格式非法");
        }
    }

    /** JSON 字符串转社交链接 Map */
    private java.util.Map<String, String> fromJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return java.util.Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return java.util.Collections.emptyMap();
        }
    }

    private String desensitizePhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
