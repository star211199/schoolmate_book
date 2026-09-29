package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群聊 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {
}
