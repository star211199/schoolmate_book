package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
