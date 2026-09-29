package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群成员 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {
}
