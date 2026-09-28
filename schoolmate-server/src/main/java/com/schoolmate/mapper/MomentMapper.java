package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Moment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级动态 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface MomentMapper extends BaseMapper<Moment> {
}
