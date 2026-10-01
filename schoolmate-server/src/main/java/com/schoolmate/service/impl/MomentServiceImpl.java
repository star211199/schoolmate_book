package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.moment.CommentCreateDTO;
import com.schoolmate.dto.moment.MomentCreateDTO;
import com.schoolmate.entity.Comment;
import com.schoolmate.entity.Moment;
import com.schoolmate.entity.MomentLike;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.CommentMapper;
import com.schoolmate.mapper.MomentLikeMapper;
import com.schoolmate.mapper.MomentMapper;
import com.schoolmate.service.ClassService;
import com.schoolmate.service.MomentService;
import com.schoolmate.service.NotificationService;
import com.schoolmate.service.UserService;
import com.schoolmate.utils.FileUploadUtil;
import com.schoolmate.vo.moment.CommentVO;
import com.schoolmate.vo.moment.MomentVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 班级动态 / 时间轴服务实现。
 *
 * @author Albot
 */
@Slf4j
@Service
public class MomentServiceImpl extends ServiceImpl<MomentMapper, Moment> implements MomentService {

    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_PASSED = "PASSED";

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private MomentLikeMapper momentLikeMapper;

    @Resource
    private ClassService classService;

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${schoolmate.content.audit-enabled}")
    private boolean auditEnabled;

    @Override
    public PageResult<MomentVO> pageMoments(Long classId, Long pageNum, Long pageSize) {
        IPage<Moment> page = this.lambdaQuery()
            .eq(Moment::getClassId, classId)
            .eq(Moment::getAuditStatus, AUDIT_PASSED)
            .orderByDesc(Moment::getCreateTime)
            .page(new Page<>(pageNum, pageSize));

        List<Moment> records = page.getRecords();
        Map<Long, User> userMap = records.isEmpty() ? Map.of()
            : userService.listByIds(records.stream().map(Moment::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(),
            records.stream().map(m -> convertToVO(m, userMap.get(m.getUserId()))).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentVO createMoment(Long classId, MomentCreateDTO dto) {
        Long userId = UserContext.getUserId();
        classService.assertMember(classId, userId);

        Moment moment = new Moment();
        moment.setClassId(classId);
        moment.setUserId(userId);
        moment.setContent(dto.getContent());
        moment.setImageUrls(toJson(dto.getImageUrls()));
        moment.setAuditStatus(auditEnabled ? AUDIT_PENDING : AUDIT_PASSED);
        this.save(moment);

        return convertToVO(moment, userService.getById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentVO updateMoment(Long id, MomentCreateDTO dto) {
        Moment moment = this.getById(id);
        if (moment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        if (!Objects.equals(moment.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己发布的动态");
        }
        moment.setContent(dto.getContent());
        moment.setImageUrls(toJson(dto.getImageUrls()));
        this.updateById(moment);
        return convertToVO(moment, userService.getById(moment.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMoment(Long id) {
        Moment moment = this.getById(id);
        if (moment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        if (!Objects.equals(moment.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的动态");
        }
        this.removeById(id);
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getMomentId, id));
    }

    @Override
    public List<CommentVO> listComments(Long momentId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getMomentId, momentId)
            .orderByAsc(Comment::getCreateTime));
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().toList();
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        return comments.stream().map(c -> convertToCommentVO(c, userMap.get(c.getUserId()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long momentId, CommentCreateDTO dto) {
        Long userId = UserContext.getUserId();
        Moment moment = this.getById(momentId);
        if (moment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        classService.assertMember(moment.getClassId(), userId);

        Comment comment = new Comment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        commentMapper.insert(comment);

        // 通知中心：回复评论时通知被回复者，否则通知动态作者；自己评论自己不通知
        User commenter = userService.getById(userId);
        String commenterName = displayName(commenter);
        if (comment.getParentId() != null) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                notificationService.notify(parent.getUserId(), NotificationService.TYPE_MOMENT_COMMENT,
                    commenterName + " 回复了你的评论", dto.getContent(),
                    NotificationService.BIZ_MOMENT, moment.getClassId(), userId);
            }
        } else {
            notificationService.notify(moment.getUserId(), NotificationService.TYPE_MOMENT_COMMENT,
                commenterName + " 评论了你的动态", dto.getContent(),
                NotificationService.BIZ_MOMENT, moment.getClassId(), userId);
        }

        return convertToCommentVO(comment, commenter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentVO toggleLike(Long momentId) {
        Long userId = UserContext.getUserId();
        Moment moment = this.getById(momentId);
        if (moment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        classService.assertMember(moment.getClassId(), userId);

        // 幂等切换：已点赞则取消，未点赞则新增
        LambdaQueryWrapper<MomentLike> wrapper = new LambdaQueryWrapper<MomentLike>()
            .eq(MomentLike::getMomentId, momentId)
            .eq(MomentLike::getUserId, userId);
        MomentLike existing = momentLikeMapper.selectOne(wrapper);
        if (existing != null) {
            momentLikeMapper.deleteById(existing.getId());
        } else {
            MomentLike like = new MomentLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            momentLikeMapper.insert(like);

            // 通知中心：仅「点赞」时通知动态作者，取消点赞不通知（自己赞自己也不通知）
            // bizId 存 classId，前端点击通知可直接跳到班级主页的动态区
            notificationService.notify(moment.getUserId(), NotificationService.TYPE_MOMENT_LIKE,
                displayName(userService.getById(userId)) + " 赞了你的动态", null,
                NotificationService.BIZ_MOMENT, moment.getClassId(), userId);
        }

        MomentVO vo = convertToVO(moment, userService.getById(moment.getUserId()));
        return vo;
    }

    /** 展示名：优先昵称，其次账号，都拿不到时兜底「有人」 */
    private String displayName(User user) {
        if (user == null) {
            return "有人";
        }
        return user.getNickname() != null && !user.getNickname().isEmpty()
            ? user.getNickname() : user.getUsername();
    }

    /** 图片 URL 列表序列化为 JSON 字符串 */
    private String toJson(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            log.warn("图片列表序列化失败", e);
            return null;
        }
    }

    private MomentVO convertToVO(Moment moment, User user) {
        if (Objects.isNull(moment)) {
            return null;
        }
        MomentVO vo = new MomentVO();
        vo.setId(moment.getId());
        vo.setClassId(moment.getClassId());
        vo.setUserId(moment.getUserId());
        vo.setContent(moment.getContent());
        vo.setAuditStatus(moment.getAuditStatus());
        vo.setCreateTime(moment.getCreateTime());
        vo.setImageUrls(FileUploadUtil.parseImageUrls(moment.getImageUrls()));
        Long count = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getMomentId, moment.getId()));
        vo.setCommentCount(count == null ? 0L : count);

        // 点赞数与当前用户点赞状态
        Long likeCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
            .eq(MomentLike::getMomentId, moment.getId()));
        vo.setLikeCount(likeCount == null ? 0L : likeCount);
        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            Long myLike = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
                .eq(MomentLike::getMomentId, moment.getId())
                .eq(MomentLike::getUserId, currentUserId));
            vo.setLiked(myLike != null && myLike > 0);
        } else {
            vo.setLiked(false);
        }
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }

    private CommentVO convertToCommentVO(Comment comment, User user) {
        if (Objects.isNull(comment)) {
            return null;
        }
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setMomentId(comment.getMomentId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setCreateTime(comment.getCreateTime());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}
