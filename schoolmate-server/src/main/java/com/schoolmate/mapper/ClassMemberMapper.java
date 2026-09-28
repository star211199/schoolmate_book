package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ClassMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级成员 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ClassMemberMapper extends BaseMapper<ClassMember> {
}
