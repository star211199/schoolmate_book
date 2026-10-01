package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.ResultCode;
import com.schoolmate.dto.capsule.CapsuleCreateDTO;
import com.schoolmate.entity.TimeCapsule;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.TimeCapsuleMapper;
import com.schoolmate.service.CapsuleService;
import com.schoolmate.service.ClassService;
import com.schoolmate.service.NotificationService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.capsule.CapsuleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 时光胶囊服务实现。
 *
 * <p>两个核心设计：
 * <ul>
 *   <li><b>内容可见性用实时时间判断</b>（{@code now >= openTime}），不依赖 status 字段，
 *       因此定时任务尚未跑到时，到点的信也能立即打开。</li>
 *   <li><b>status 字段只服务于「到期通知」</b>：{@link #openDueCapsules()} 每分钟扫描一次，
 *       把刚跨过开启时间的信置为 OPENED 并通知写信人 —— 通知必须有一个「从未知到已知」的
 *       状态跃迁作触发点，这正是 status 存在的意义。</li>
 * </ul>
 *
 * @author Albot
 */
@Slf4j
@Service
public class CapsuleServiceImpl extends ServiceImpl<TimeCapsuleMapper, TimeCapsule> implements CapsuleService {

    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_OPENED = "OPENED";

    public static final String TYPE_SELF = "SELF";
    public static final String TYPE_PUBLIC = "PUBLIC";

    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, CapsuleCreateDTO dto) {
        if (TYPE_PUBLIC.equals(dto.getOpenType())) {
            if (dto.getClassId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "写给全班时必须指定班级");
            }
            // 只有本班成员才能往班级墙上投信
            classService.assertMember(dto.getClassId(), userId);
        }

        TimeCapsule capsule = new TimeCapsule();
        capsule.setUserId(userId);
        capsule.setClassId(TYPE_PUBLIC.equals(dto.getOpenType()) ? dto.getClassId() : null);
        capsule.setTitle(dto.getTitle());
        capsule.setContent(dto.getContent());
        capsule.setOpenTime(dto.getOpenTime());
        capsule.setOpenType(dto.getOpenType());
        capsule.setStatus(STATUS_SEALED);
        this.save(capsule);
        return capsule.getId();
    }

    @Override
    public List<CapsuleVO> listMine(Long userId) {
        List<TimeCapsule> capsules = this.list(new LambdaQueryWrapper<TimeCapsule>()
                .eq(TimeCapsule::getUserId, userId)
                // 未到期的排在前面（最近要开启的最靠前），已开启的按开启时间倒序
                .orderByAsc(TimeCapsule::getOpenTime));
        User me = userService.getById(userId);
        return capsules.stream().map(c -> toVO(c, me, true)).toList();
    }

    @Override
    public List<CapsuleVO> listClassPublic(Long userId, Long classId) {
        classService.assertMember(classId, userId);
        List<TimeCapsule> capsules = this.list(new LambdaQueryWrapper<TimeCapsule>()
                .eq(TimeCapsule::getClassId, classId)
                .eq(TimeCapsule::getOpenType, TYPE_PUBLIC)
                .orderByDesc(TimeCapsule::getOpenTime));
        if (capsules.isEmpty()) {
            return List.of();
        }
        List<Long> authorIds = capsules.stream().map(TimeCapsule::getUserId).distinct().toList();
        Map<Long, User> authorMap = userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return capsules.stream().map(c -> toVO(c, authorMap.get(c.getUserId()), false)).toList();
    }

    @Override
    public CapsuleVO detail(Long userId, Long id) {
        TimeCapsule capsule = this.getById(id);
        if (capsule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "胶囊不存在");
        }
        boolean isOwner = Objects.equals(capsule.getUserId(), userId);
        if (TYPE_SELF.equals(capsule.getOpenType()) && !isOwner) {
            // 写给自己的信，连存在性都不应该让别人知道
            throw new BusinessException(ResultCode.NOT_FOUND, "胶囊不存在");
        }
        if (TYPE_PUBLIC.equals(capsule.getOpenType())) {
            classService.assertMember(capsule.getClassId(), userId);
        }
        return toVO(capsule, userService.getById(capsule.getUserId()), isOwner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        TimeCapsule capsule = this.getById(id);
        if (capsule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "胶囊不存在");
        }
        if (!Objects.equals(capsule.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的胶囊");
        }
        if (STATUS_OPENED.equals(capsule.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "信件已开启，不允许删除");
        }
        this.removeById(id);
    }

    /**
     * 每分钟扫描一次到点胶囊。
     *
     * <p>单节点部署下 @Scheduled 天然串行，无需分布式锁；
     * 未来集群化时这里要换成带 ShedLock / 数据库行锁的实现。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void openDueCapsules() {
        List<TimeCapsule> due = this.list(new LambdaQueryWrapper<TimeCapsule>()
                .eq(TimeCapsule::getStatus, STATUS_SEALED)
                .le(TimeCapsule::getOpenTime, LocalDateTime.now()));
        if (due.isEmpty()) {
            return;
        }
        for (TimeCapsule capsule : due) {
            capsule.setStatus(STATUS_OPENED);
            this.updateById(capsule);
            notificationService.notify(capsule.getUserId(), NotificationService.TYPE_CAPSULE_OPENED,
                    "你的时光胶囊「" + capsule.getTitle() + "」已到期开启", null,
                    NotificationService.BIZ_CAPSULE, capsule.getId(), null);
        }
        log.info("时光胶囊到期开启 {} 封", due.size());
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 组装出参。content 仅在「已到开启时间」时返回，未到期一律为 null。
     *
     * @param owner 当前查看人是否为写信人（预留：未来若支持「未到期可读自己写的信」可据此区分）
     */
    private CapsuleVO toVO(TimeCapsule capsule, User author, boolean owner) {
        CapsuleVO vo = new CapsuleVO();
        vo.setId(capsule.getId());
        vo.setUserId(capsule.getUserId());
        if (author != null) {
            vo.setNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
            vo.setAvatar(author.getAvatar());
        }
        vo.setClassId(capsule.getClassId());
        vo.setTitle(capsule.getTitle());
        vo.setOpenTime(capsule.getOpenTime());
        vo.setOpenType(capsule.getOpenType());
        vo.setStatus(capsule.getStatus());
        vo.setCreateTime(capsule.getCreateTime());

        LocalDateTime now = LocalDateTime.now();
        boolean openable = !now.isBefore(capsule.getOpenTime());
        vo.setOpenable(openable);
        vo.setCountdownSeconds(openable ? 0L : Duration.between(now, capsule.getOpenTime()).getSeconds());
        // 核心约束：未到点，内容连写信人自己也看不到 —— 这才是「时光胶囊」
        vo.setContent(openable ? capsule.getContent() : null);
        return vo;
    }
}
