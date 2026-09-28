package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.common.PageResult;
import com.schoolmate.dto.user.PasswordUpdateDTO;
import com.schoolmate.dto.user.ProfileUpdateDTO;
import com.schoolmate.dto.user.UserUpdateDTO;
import com.schoolmate.entity.User;
import com.schoolmate.vo.user.UserProfileVO;
import com.schoolmate.vo.user.UserVO;

/**
 * 用户服务接口。
 *
 * @author Albot
 */
public interface UserService extends IService<User> {

    /**
     * 查询用户公开信息。
     */
    UserVO getUserById(Long id);

    /**
     * 更新用户账号信息（仅本人）。
     */
    UserVO updateUser(Long id, UserUpdateDTO dto);

    /**
     * 修改密码。
     */
    void updatePassword(Long id, PasswordUpdateDTO dto);

    /**
     * 查询个人主页资料。
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新个人主页资料。
     */
    UserProfileVO updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * 管理员分页查询用户。
     */
    PageResult<UserVO> pageUsers(Long pageNum, Long pageSize, String keyword);

    /**
     * 管理员启停用户。
     */
    void updateStatus(Long id, String status);
}
