package com.schoolmate.controller.admin;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.service.AdminService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.admin.AuditVO;
import com.schoolmate.vo.admin.StatsVO;
import com.schoolmate.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理控制器（仅管理员可访问）。
 *
 * @author Albot
 */
@Tag(name = "后台管理", description = "用户管理、内容审核、数据统计")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private AdminService adminService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> pageUsers(
        @RequestParam(defaultValue = "1") Long pageNum,
        @RequestParam(defaultValue = "10") Long pageSize,
        @RequestParam(required = false) String keyword) {
        assertAdmin();
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        assertAdmin();
        userService.updateStatus(id, status);
        return Result.success(null, "操作成功");
    }

    @Operation(summary = "重置用户密码（返回随机新密码，仅显示一次，请线下告知用户）")
    @PutMapping("/users/{id}/reset-password")
    public Result<String> resetUserPassword(@PathVariable Long id) {
        assertAdmin();
        return Result.success(userService.resetPasswordByAdmin(id), "密码已重置，请将新密码告知用户并提醒尽快修改");
    }

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        assertAdmin();
        return Result.success(adminService.getStats());
    }

    @Operation(summary = "查询待审核内容")
    @GetMapping("/audit/pending")
    public Result<List<AuditVO>> pendingAudits(@RequestParam(required = false) String type) {
        assertAdmin();
        return Result.success(adminService.listPendingAudits(type));
    }

    @Operation(summary = "审核内容（passed=true 通过，false 驳回）")
    @PutMapping("/audit/{type}/{id}")
    public Result<Void> audit(@PathVariable String type,
                              @PathVariable Long id,
                              @RequestParam(defaultValue = "true") Boolean passed) {
        assertAdmin();
        adminService.audit(type, id, passed);
        return Result.success(null, passed ? "审核通过" : "已驳回");
    }

    /** 管理员权限校验 */
    private void assertAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可执行该操作");
        }
    }
}
