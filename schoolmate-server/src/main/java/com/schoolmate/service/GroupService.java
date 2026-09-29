package com.schoolmate.service;

import com.schoolmate.common.PageResult;
import com.schoolmate.dto.chat.GroupCreateDTO;
import com.schoolmate.dto.chat.GroupUpdateDTO;
import com.schoolmate.vo.chat.ChatGroupVO;
import com.schoolmate.vo.chat.GroupMemberVO;

import java.util.List;

/**
 * 群聊服务。
 *
 * @author Albot
 */
public interface GroupService {

    /**
     * 确保指定班级的班级群存在（幂等）。
     *
     * <p>由 ClassService 在创建班级时调用。内部先查后建，并由数据库唯一索引兜底，
     * 因此并发调用也只会产生一个群。
     *
     * @return 群ID
     */
    Long ensureClassGroup(Long classId);

    /**
     * 班级成员变动时同步到班级群（加入 / 退出）。
     *
     * @param join true 表示加入班级，false 表示退出班级
     */
    void syncClassMember(Long classId, Long userId, boolean join);

    /** 创建自建群 */
    Long createGroup(Long userId, GroupCreateDTO dto);

    /** 群资料（含我在群内的角色） */
    ChatGroupVO getGroupDetail(Long userId, Long groupId);

    /** 我加入的所有群 */
    List<ChatGroupVO> listMyGroups(Long userId);

    /** 修改群名 / 公告 / 头像，需群主或管理员 */
    ChatGroupVO updateGroup(Long userId, Long groupId, GroupUpdateDTO dto);

    /** 解散群（仅群主，且仅自建群） */
    void dismissGroup(Long userId, Long groupId);

    /** 群成员分页列表 */
    PageResult<GroupMemberVO> pageMembers(Long userId, Long groupId, Long pageNum, Long pageSize);

    /** 批量拉人入群 */
    void addMembers(Long userId, Long groupId, List<Long> userIds);

    /** 移出成员（群主/管理员），也可用于自己退群 */
    void removeMember(Long userId, Long groupId, Long targetUserId);

    /** 主动退群 */
    void quitGroup(Long userId, Long groupId);

    /** 班级被删除时同步解散对应的班级群 */
    void dismissClassGroup(Long classId);

    /** 校验用户是否为群成员，不是则抛业务异常 */
    void assertGroupMember(Long userId, Long groupId);
}
