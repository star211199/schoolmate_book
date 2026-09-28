package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.dto.user.PasswordUpdateDTO;
import com.schoolmate.dto.user.ProfileUpdateDTO;
import com.schoolmate.dto.user.UserUpdateDTO;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.user.UserProfileVO;
import com.schoolmate.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器。
 *
 * @author Albot
 */
@Tag(name = "用户管理", description = "用户信息、个人主页资料、修改密码")
@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "查询用户公开信息")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "修改用户账号信息（仅本人或管理员）")
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userService.updateUser(id, dto), "修改成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(id, dto);
        return Result.success(null, "密码修改成功");
    }

    @Operation(summary = "查看个人主页资料")
    @GetMapping("/{id}/profile")
    public Result<UserProfileVO> getProfile(@PathVariable Long id) {
        return Result.success(userService.getProfile(id));
    }

    @Operation(summary = "编辑个人主页资料")
    @PutMapping("/{id}/profile")
    public Result<UserProfileVO> updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(id, dto), "保存成功");
    }
}
