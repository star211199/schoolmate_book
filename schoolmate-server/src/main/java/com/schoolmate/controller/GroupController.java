package com.schoolmate.controller;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.chat.GroupCreateDTO;
import com.schoolmate.dto.chat.GroupMemberAddDTO;
import com.schoolmate.dto.chat.GroupUpdateDTO;
import com.schoolmate.service.GroupService;
import com.schoolmate.vo.chat.ChatGroupVO;
import com.schoolmate.vo.chat.GroupMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 群聊控制器。
 *
 * @author Albot
 */
@Tag(name = "群聊", description = "建群、群资料、群成员管理")
@RestController
@RequestMapping("/groups")
public class GroupController {

    @Resource
    private GroupService groupService;

    @Operation(summary = "我加入的群列表")
    @GetMapping
    public Result<List<ChatGroupVO>> listMyGroups() {
        return Result.success(groupService.listMyGroups(UserContext.getUserId()));
    }

    @Operation(summary = "创建群聊（自己自动成为群主）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GroupCreateDTO dto) {
        return Result.success(groupService.createGroup(UserContext.getUserId(), dto), "群创建成功");
    }

    @Operation(summary = "群资料（含我在群内的角色）")
    @GetMapping("/{groupId}")
    public Result<ChatGroupVO> detail(@PathVariable Long groupId) {
        return Result.success(groupService.getGroupDetail(UserContext.getUserId(), groupId));
    }

    @Operation(summary = "修改群名/公告/头像，需群主或管理员")
    @PutMapping("/{groupId}")
    public Result<ChatGroupVO> update(@PathVariable Long groupId, @Valid @RequestBody GroupUpdateDTO dto) {
        return Result.success(groupService.updateGroup(UserContext.getUserId(), groupId, dto), "修改成功");
    }

    @Operation(summary = "解散群（仅群主，且仅自建群）")
    @DeleteMapping("/{groupId}")
    public Result<Void> dismiss(@PathVariable Long groupId) {
        groupService.dismissGroup(UserContext.getUserId(), groupId);
        return Result.success();
    }

    @Operation(summary = "群成员分页列表（群主在前）")
    @GetMapping("/{groupId}/members")
    public Result<PageResult<GroupMemberVO>> members(@PathVariable Long groupId,
                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                     @RequestParam(defaultValue = "20") Long pageSize) {
        return Result.success(groupService.pageMembers(UserContext.getUserId(), groupId, pageNum, pageSize));
    }

    @Operation(summary = "批量拉人入群，需群主或管理员")
    @PostMapping("/{groupId}/members")
    public Result<Void> addMembers(@PathVariable Long groupId, @Valid @RequestBody GroupMemberAddDTO dto) {
        groupService.addMembers(UserContext.getUserId(), groupId, dto.getUserIds());
        return Result.success(null, "已添加成员");
    }

    @Operation(summary = "移出成员（传自己的ID等同于退群）")
    @DeleteMapping("/{groupId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(UserContext.getUserId(), groupId, userId);
        return Result.success();
    }

    @Operation(summary = "退出群聊（班级群不可退，需先退出班级）")
    @PostMapping("/{groupId}/quit")
    public Result<Void> quit(@PathVariable Long groupId) {
        groupService.quitGroup(UserContext.getUserId(), groupId);
        return Result.success(null, "已退出群聊");
    }
}
