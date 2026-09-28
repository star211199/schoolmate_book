package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.message.MessageCreateDTO;
import com.schoolmate.entity.Message;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.MessageMapper;
import com.schoolmate.service.ClassService;
import com.schoolmate.service.MessageService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.message.MessageVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 留言服务实现。
 *
 * @author Albot
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_PASSED = "PASSED";

    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    @Value("${schoolmate.content.audit-enabled}")
    private boolean auditEnabled;

    @Override
    public PageResult<MessageVO> pageMessages(Long classId, Long pageNum, Long pageSize) {
        IPage<Message> page = this.lambdaQuery()
            .eq(Message::getClassId, classId)
            .eq(Message::getAuditStatus, AUDIT_PASSED)
            .orderByDesc(Message::getCreateTime)
            .page(new Page<>(pageNum, pageSize));

        List<Message> records = page.getRecords();
        Map<Long, User> userMap = records.isEmpty() ? Map.of()
            : userService.listByIds(records.stream().map(Message::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(),
            records.stream().map(m -> convertToVO(m, userMap.get(m.getUserId()))).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO createMessage(Long classId, MessageCreateDTO dto) {
        Long userId = UserContext.getUserId();
        classService.assertMember(classId, userId);

        Message message = new Message();
        message.setClassId(classId);
        message.setUserId(userId);
        message.setContent(dto.getContent());
        message.setAuditStatus(auditEnabled ? AUDIT_PENDING : AUDIT_PASSED);
        this.save(message);

        User user = userService.getById(userId);
        return convertToVO(message, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id) {
        Message message = this.getById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "留言不存在");
        }
        if (!Objects.equals(message.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的留言");
        }
        this.removeById(id);
    }

    private MessageVO convertToVO(Message message, User user) {
        if (Objects.isNull(message)) {
            return null;
        }
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setClassId(message.getClassId());
        vo.setUserId(message.getUserId());
        vo.setContent(message.getContent());
        vo.setAuditStatus(message.getAuditStatus());
        vo.setCreateTime(message.getCreateTime());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}
