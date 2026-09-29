package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
