package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 留言 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
