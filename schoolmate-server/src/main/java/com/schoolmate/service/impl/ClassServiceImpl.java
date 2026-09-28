package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.cls.ClassCreateDTO;
import com.schoolmate.dto.cls.ClassUpdateDTO;
import com.schoolmate.dto.cls.JoinClassDTO;
import com.schoolmate.entity.ClassInfo;
import com.schoolmate.entity.ClassMember;
import com.schoolmate.entity.User;
import com.schoolmate.entity.UserProfile;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.ClassInfoMapper;
import com.schoolmate.mapper.ClassMemberMapper;
import com.schoolmate.service.ClassService;
import com.schoolmate.service.UserProfileService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.cls.ClassMemberVO;
import com.schoolmate.vo.cls.ClassVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 班级服务实现。
 *
 * @author Albot
 */
@Service
public class ClassServiceImpl extends ServiceImpl<ClassInfoMapper, ClassInfo> implements ClassService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";

    @Resource
    private ClassMemberMapper classMemberMapper;

    @Resource
    private UserService userService;

    @Resource
    private UserProfileService userProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO createClass(ClassCreateDTO dto) {
        Long userId = UserContext.getUserId();

        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(dto.getClassName());
        classInfo.setGrade(dto.getGrade());
        classInfo.setMajor(dto.getMajor());
        classInfo.setDescription(dto.getDescription());
        classInfo.setInviteCode(generateInviteCode());
        classInfo.setOwnerId(userId);
        this.save(classInfo);

        // 创建者自动成为 OWNER 成员
        addMember(classInfo.getId(), userId, ROLE_OWNER);

        return fillExtra(this.getById(classInfo.getId()));
    }

    @Override
    public PageResult<ClassVO> pageClasses(Long pageNum, Long pageSize, String keyword) {
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ClassInfo::getClassName, keyword)
                .or().like(ClassInfo::getMajor, keyword);
        }
        wrapper.orderByDesc(ClassInfo::getCreateTime);
        IPage<ClassInfo> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, this::fillExtra);
    }

    @Override
    public ClassVO getClassDetail(Long id) {
        ClassInfo classInfo = this.getById(id);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        return fillExtra(classInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO updateClass(Long id, ClassUpdateDTO dto) {
        ClassInfo classInfo = this.getById(id);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        assertOwnerOrAdmin(classInfo);
        classInfo.setClassName(dto.getClassName());
        classInfo.setGrade(dto.getGrade());
        classInfo.setGraduationDate(dto.getGraduationDate());
        classInfo.setMajor(dto.getMajor());
        classInfo.setDescription(dto.getDescription());
        this.updateById(classInfo);
        return fillExtra(classInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long id) {
        ClassInfo classInfo = this.getById(id);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        assertOwnerOrAdmin(classInfo);
        this.removeById(id);
        // 同步移除成员关系
        classMemberMapper.delete(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO joinClass(JoinClassDTO dto) {
        Long userId = UserContext.getUserId();
        ClassInfo classInfo = this.getOne(new LambdaQueryWrapper<ClassInfo>()
            .eq(ClassInfo::getInviteCode, dto.getInviteCode()));
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请码无效");
        }
        Long exist = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classInfo.getId())
            .eq(ClassMember::getUserId, userId));
        if (exist != null && exist > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "你已加入该班级");
        }
        addMember(classInfo.getId(), userId, ROLE_MEMBER);
        return fillExtra(classInfo);
    }

    @Override
    public List<ClassMemberVO> listMembers(Long classId) {
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classId)
            .orderByAsc(ClassMember::getJoinTime));
        List<Long> userIds = members.stream().map(ClassMember::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }

        // 批量查询用户与资料，避免 N+1
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, UserProfile> profileMap = userProfileService.list(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds)).stream()
            .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        return members.stream().map(member -> {
            User user = userMap.get(member.getUserId());
            UserProfile profile = profileMap.get(member.getUserId());
            ClassMemberVO vo = new ClassMemberVO();
            vo.setId(member.getId());
            vo.setClassId(member.getClassId());
            vo.setUserId(member.getUserId());
            vo.setMemberRole(member.getMemberRole());
            vo.setJoinTime(member.getJoinTime());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            if (profile != null) {
                vo.setRealName(profile.getRealName());
                vo.setConstellation(profile.getConstellation());
                vo.setMbti(profile.getMbti());
                vo.setMotto(profile.getMotto());
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long classId, Long userId) {
        ClassInfo classInfo = this.getById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
        assertOwnerOrAdmin(classInfo);
        if (Objects.equals(classInfo.getOwnerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移除班级创建者");
        }
        classMemberMapper.delete(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classId)
            .eq(ClassMember::getUserId, userId));
    }

    @Override
    public List<ClassVO> listMyClasses(Long userId) {
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> classIds = members.stream().map(ClassMember::getClassId).toList();
        return this.listByIds(classIds).stream().map(this::fillExtra).toList();
    }

    @Override
    public void assertMember(Long classId, Long userId) {
        Long count = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classId)
            .eq(ClassMember::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不是该班级成员，无法执行该操作");
        }
    }

    @Override
    public List<com.schoolmate.vo.cls.BirthdayReminderVO> birthdayReminders(Long classId, int withinDays) {
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = members.stream().map(ClassMember::getUserId).toList();
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        List<UserProfile> profiles = userProfileService.list(
            new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds));

        LocalDate today = LocalDate.now();
        return profiles.stream()
            .filter(p -> p.getBirthday() != null)
            .map(p -> {
                // 计算今年生日，若已过则顺延到明年
                LocalDate next = p.getBirthday().withYear(today.getYear());
                if (next.isBefore(today)) {
                    next = next.plusYears(1);
                }
                long days = ChronoUnit.DAYS.between(today, next);
                User user = userMap.get(p.getUserId());
                com.schoolmate.vo.cls.BirthdayReminderVO vo = new com.schoolmate.vo.cls.BirthdayReminderVO();
                vo.setUserId(p.getUserId());
                vo.setBirthday(p.getBirthday());
                vo.setConstellation(p.getConstellation());
                vo.setDaysUntil(days);
                vo.setToday(days == 0);
                if (user != null) {
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                }
                return vo;
            })
            .filter(vo -> vo.getDaysUntil() <= withinDays)
            .sorted(java.util.Comparator.comparing(com.schoolmate.vo.cls.BirthdayReminderVO::getDaysUntil))
            .toList();
    }

    /** 新增成员记录 */
    private void addMember(Long classId, Long userId, String role) {
        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(userId);
        member.setMemberRole(role);
        classMemberMapper.insert(member);
    }

    /** 生成 6 位邀请码 */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    /** 越权校验：仅班级创建者或管理员 */
    private void assertOwnerOrAdmin(ClassInfo classInfo) {
        Long currentUserId = UserContext.getUserId();
        boolean isOwner = Objects.equals(classInfo.getOwnerId(), currentUserId);
        if (!isOwner && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有班级创建者或管理员可执行该操作");
        }
    }

    /** 填充成员数量与当前用户角色 */
    private ClassVO fillExtra(ClassInfo classInfo) {
        if (Objects.isNull(classInfo)) {
            return null;
        }
        ClassVO vo = new ClassVO();
        vo.setId(classInfo.getId());
        vo.setClassName(classInfo.getClassName());
        vo.setGrade(classInfo.getGrade());
        vo.setGraduationDate(classInfo.getGraduationDate());
        if (classInfo.getGraduationDate() != null) {
            vo.setDaysToGraduation(ChronoUnit.DAYS.between(LocalDate.now(), classInfo.getGraduationDate()));
        }
        vo.setMajor(classInfo.getMajor());
        vo.setDescription(classInfo.getDescription());
        vo.setInviteCode(classInfo.getInviteCode());
        vo.setOwnerId(classInfo.getOwnerId());
        vo.setCreateTime(classInfo.getCreateTime());

        Long memberCount = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
            .eq(ClassMember::getClassId, classInfo.getId()));
        vo.setMemberCount(memberCount == null ? 0L : memberCount);

        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classInfo.getId())
                .eq(ClassMember::getUserId, currentUserId));
            vo.setCurrentUserRole(member == null ? null : member.getMemberRole());
        }
        return vo;
    }
}
