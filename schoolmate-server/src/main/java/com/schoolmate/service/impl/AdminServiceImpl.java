package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schoolmate.common.ResultCode;
import com.schoolmate.entity.Message;
import com.schoolmate.entity.Moment;
import com.schoolmate.entity.Photo;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.AlbumMapper;
import com.schoolmate.mapper.ClassInfoMapper;
import com.schoolmate.mapper.MessageMapper;
import com.schoolmate.mapper.MomentMapper;
import com.schoolmate.mapper.PhotoMapper;
import com.schoolmate.service.AdminService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.admin.AuditVO;
import com.schoolmate.vo.admin.StatsVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 后台管理服务实现。
 *
 * @author Albot
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_PASSED = "PASSED";
    private static final String AUDIT_REJECTED = "REJECTED";

    private static final String TYPE_MESSAGE = "MESSAGE";
    private static final String TYPE_MOMENT = "MOMENT";
    private static final String TYPE_PHOTO = "PHOTO";

    @Resource
    private UserService userService;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private MomentMapper momentMapper;

    @Resource
    private PhotoMapper photoMapper;

    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private ClassInfoMapper classInfoMapper;

    @Override
    public StatsVO getStats() {
        StatsVO vo = new StatsVO();
        vo.setUserCount(userService.count());
        vo.setClassCount(classInfoMapper.selectCount(null));
        vo.setMessageCount(messageMapper.selectCount(null));
        vo.setAlbumCount(albumMapper.selectCount(null));
        vo.setPhotoCount(photoMapper.selectCount(null));
        vo.setMomentCount(momentMapper.selectCount(null));
        vo.setPendingCount(
            nullSafe(messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getAuditStatus, AUDIT_PENDING)))
                + nullSafe(momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getAuditStatus, AUDIT_PENDING)))
                + nullSafe(photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAuditStatus, AUDIT_PENDING))));
        return vo;
    }

    @Override
    public List<AuditVO> listPendingAudits(String type) {
        List<AuditVO> result = new ArrayList<>();

        if (!StringUtils.hasText(type) || TYPE_MESSAGE.equals(type)) {
            List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getAuditStatus, AUDIT_PENDING)
                .orderByDesc(Message::getCreateTime));
            Map<Long, User> userMap = toUserMap(messages.stream().map(Message::getUserId).distinct().toList());
            messages.forEach(m -> {
                AuditVO vo = new AuditVO();
                vo.setId(m.getId());
                vo.setType(TYPE_MESSAGE);
                vo.setContent(m.getContent());
                vo.setUserId(m.getUserId());
                vo.setAuditStatus(m.getAuditStatus());
                vo.setCreateTime(m.getCreateTime());
                User user = userMap.get(m.getUserId());
                vo.setNickname(user == null ? null : user.getNickname());
                result.add(vo);
            });
        }

        if (!StringUtils.hasText(type) || TYPE_MOMENT.equals(type)) {
            List<Moment> moments = momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getAuditStatus, AUDIT_PENDING)
                .orderByDesc(Moment::getCreateTime));
            Map<Long, User> userMap = toUserMap(moments.stream().map(Moment::getUserId).distinct().toList());
            moments.forEach(m -> {
                AuditVO vo = new AuditVO();
                vo.setId(m.getId());
                vo.setType(TYPE_MOMENT);
                vo.setContent(m.getContent());
                vo.setUserId(m.getUserId());
                vo.setAuditStatus(m.getAuditStatus());
                vo.setCreateTime(m.getCreateTime());
                User user = userMap.get(m.getUserId());
                vo.setNickname(user == null ? null : user.getNickname());
                result.add(vo);
            });
        }

        if (!StringUtils.hasText(type) || TYPE_PHOTO.equals(type)) {
            List<Photo> photos = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAuditStatus, AUDIT_PENDING)
                .orderByDesc(Photo::getCreateTime));
            Map<Long, User> userMap = toUserMap(photos.stream().map(Photo::getUserId).distinct().toList());
            photos.forEach(p -> {
                AuditVO vo = new AuditVO();
                vo.setId(p.getId());
                vo.setType(TYPE_PHOTO);
                vo.setUrl(p.getUrl());
                vo.setContent(p.getDescription());
                vo.setUserId(p.getUserId());
                vo.setAuditStatus(p.getAuditStatus());
                vo.setCreateTime(p.getCreateTime());
                User user = userMap.get(p.getUserId());
                vo.setNickname(user == null ? null : user.getNickname());
                result.add(vo);
            });
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(String type, Long id, boolean passed) {
        String status = passed ? AUDIT_PASSED : AUDIT_REJECTED;
        switch (type.toUpperCase()) {
            case TYPE_MESSAGE -> {
                Message message = messageMapper.selectById(id);
                if (message == null) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "留言不存在");
                }
                message.setAuditStatus(status);
                messageMapper.updateById(message);
            }
            case TYPE_MOMENT -> {
                Moment moment = momentMapper.selectById(id);
                if (moment == null) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
                }
                moment.setAuditStatus(status);
                momentMapper.updateById(moment);
            }
            case TYPE_PHOTO -> {
                Photo photo = photoMapper.selectById(id);
                if (photo == null) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "照片不存在");
                }
                photo.setAuditStatus(status);
                photoMapper.updateById(photo);
            }
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的内容类型：" + type);
        }
    }

    private Map<Long, User> toUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userService.listByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
    }

    private long nullSafe(Long value) {
        return Objects.isNull(value) ? 0L : value;
    }
}
