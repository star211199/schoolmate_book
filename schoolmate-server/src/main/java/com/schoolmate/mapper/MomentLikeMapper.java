package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.MomentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态点赞 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface MomentLikeMapper extends BaseMapper<MomentLike> {
}
