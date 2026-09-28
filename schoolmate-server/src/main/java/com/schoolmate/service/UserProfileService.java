package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.entity.UserProfile;

/**
 * 个人主页资料服务接口。
 *
 * @author Albot
 */
public interface UserProfileService extends IService<UserProfile> {

    /**
     * 初始化个人资料（注册时调用）。
     *
     * @param userId 用户ID
     */
    void initProfile(Long userId);

    /**
     * 更新个人资料（不存在则创建）。
     *
     * @param userId 用户ID
     * @param profile 资料实体
     * @return 更新后的资料
     */
    UserProfile saveOrUpdateProfile(Long userId, UserProfile profile);
}
