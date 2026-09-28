package com.schoolmate.controller;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.cls.ClassCreateDTO;
import com.schoolmate.dto.cls.ClassUpdateDTO;
import com.schoolmate.dto.cls.JoinClassDTO;
import com.schoolmate.service.ClassService;
import com.schoolmate.vo.cls.ClassMemberVO;
import com.schoolmate.vo.cls.ClassVO;
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
 * 班级控制器。
 *
 * @author Albot
 */
@Tag(name = "班级管理", description = "班级创建、加入、成员管理")
@RestController
@RequestMapping("/classes")
public class ClassController {

    @Resource
    private ClassService classService;

    @Operation(summary = "创建班级")
    @PostMapping
    public Result<ClassVO> create(@Valid @RequestBody ClassCreateDTO dto) {
        return Result.success(classService.createClass(dto), "创建成功");
    }

    @Operation(summary = "分页查询班级列表")
    @GetMapping
    public Result<PageResult<ClassVO>> page(
        @RequestParam(defaultValue = "1") Long pageNum,
        @RequestParam(defaultValue = "10") Long pageSize,
        @RequestParam(required = false) String keyword) {
        return Result.success(classService.pageClasses(pageNum, pageSize, keyword));
    }

    @Operation(summary = "查询我加入的班级")
    @GetMapping("/my")
    public Result<List<ClassVO>> myClasses() {
        return Result.success(classService.listMyClasses(UserContext.getUserId()));
    }

    @Operation(summary = "通过邀请码加入班级")
    @PostMapping("/join")
    public Result<ClassVO> join(@Valid @RequestBody JoinClassDTO dto) {
        return Result.success(classService.joinClass(dto), "加入成功");
    }

    @Operation(summary = "查询班级详情")
    @GetMapping("/{id}")
    public Result<ClassVO> detail(@PathVariable Long id) {
        return Result.success(classService.getClassDetail(id));
    }

    @Operation(summary = "更新班级信息")
    @PutMapping("/{id}")
    public Result<ClassVO> update(@PathVariable Long id, @Valid @RequestBody ClassUpdateDTO dto) {
        return Result.success(classService.updateClass(id, dto), "修改成功");
    }

    @Operation(summary = "解散班级")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.deleteClass(id);
        return Result.success(null, "班级已解散");
    }

    @Operation(summary = "查询班级成员列表")
    @GetMapping("/{id}/members")
    public Result<List<ClassMemberVO>> members(@PathVariable Long id) {
        return Result.success(classService.listMembers(id));
    }

    @Operation(summary = "移出班级成员")
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        classService.removeMember(id, userId);
        return Result.success(null, "已移出班级");
    }

    @Operation(summary = "生日提醒：未来 N 天内过生日的成员")
    @GetMapping("/{id}/birthday-reminders")
    public Result<List<com.schoolmate.vo.cls.BirthdayReminderVO>> birthdayReminders(
        @PathVariable Long id,
        @RequestParam(defaultValue = "30") int withinDays) {
        return Result.success(classService.birthdayReminders(id, withinDays));
    }
}
