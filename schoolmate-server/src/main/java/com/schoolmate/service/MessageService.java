package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.common.PageResult;
import com.schoolmate.dto.message.MessageCreateDTO;
import com.schoolmate.entity.Message;
import com.schoolmate.vo.message.MessageVO;

/**
 * 留言服务接口。
 *
 * @author Albot
 */
public interface MessageService extends IService<Message> {

    /**
     * 分页查询班级留言。
     */
    PageResult<MessageVO> pageMessages(Long classId, Long pageNum, Long pageSize);

    /**
     * 发布留言（需为班级成员）。
     */
    MessageVO createMessage(Long classId, MessageCreateDTO dto);

    /**
     * 删除留言（本人或管理员）。
     */
    void deleteMessage(Long id);
}
