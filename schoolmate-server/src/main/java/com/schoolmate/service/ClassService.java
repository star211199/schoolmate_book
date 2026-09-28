package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.common.PageResult;
import com.schoolmate.dto.cls.ClassCreateDTO;
import com.schoolmate.dto.cls.ClassUpdateDTO;
import com.schoolmate.dto.cls.JoinClassDTO;
import com.schoolmate.entity.ClassInfo;
import com.schoolmate.vo.cls.ClassMemberVO;
import com.schoolmate.vo.cls.ClassVO;

import java.util.List;

/**
 * 班级服务接口。
 *
 * @author Albot
 */
public interface ClassService extends IService<ClassInfo> {

    /**
     * 创建班级，创建者为 OWNER 并自动加入。
     */
    ClassVO createClass(ClassCreateDTO dto);

    /**
     * 分页查询班级列表。
     */
    PageResult<ClassVO> pageClasses(Long pageNum, Long pageSize, String keyword);

    /**
     * 查询班级详情。
     */
    ClassVO getClassDetail(Long id);

    /**
     * 更新班级（仅创建者或管理员）。
     */
    ClassVO updateClass(Long id, ClassUpdateDTO dto);

    /**
     * 解散班级（仅创建者或管理员）。
     */
    void deleteClass(Long id);

    /**
     * 通过邀请码加入班级。
     */
    ClassVO joinClass(JoinClassDTO dto);

    /**
     * 查询班级成员列表。
     */
    List<ClassMemberVO> listMembers(Long classId);

    /**
     * 移出班级成员（仅班级创建者或管理员）。
     */
    void removeMember(Long classId, Long userId);

    /**
     * 查询我加入的班级。
     */
    List<ClassVO> listMyClasses(Long userId);

    /**
     * 校验用户是否为班级成员，非成员抛异常。
     */
    void assertMember(Long classId, Long userId);

    /**
     * 查询班级成员中未来 N 天内过生日的人（按天数升序）。
     */
    java.util.List<com.schoolmate.vo.cls.BirthdayReminderVO> birthdayReminders(Long classId, int withinDays);
}
