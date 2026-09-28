package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Photo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照片 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {
}
