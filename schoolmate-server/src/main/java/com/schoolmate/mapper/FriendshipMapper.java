package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友关系 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {
}
