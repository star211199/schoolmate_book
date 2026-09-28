package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
