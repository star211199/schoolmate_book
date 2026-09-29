package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ChatSessionUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户会话状态 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ChatSessionUserMapper extends BaseMapper<ChatSessionUser> {
}
