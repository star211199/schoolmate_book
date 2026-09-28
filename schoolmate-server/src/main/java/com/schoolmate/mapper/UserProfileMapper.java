package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人主页资料 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
