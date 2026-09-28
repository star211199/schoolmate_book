package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.entity.UserProfile;
import com.schoolmate.mapper.UserProfileMapper;
import com.schoolmate.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人主页资料服务实现。
 *
 * @author Albot
 */
@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initProfile(Long userId) {
        Long count = this.lambdaQuery().eq(UserProfile::getUserId, userId).count();
        if (count != null && count > 0) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        this.save(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfile saveOrUpdateProfile(Long userId, UserProfile profile) {
        UserProfile exist = this.getOne(new LambdaQueryWrapper<UserProfile>()
            .eq(UserProfile::getUserId, userId));
        if (exist == null) {
            profile.setUserId(userId);
            this.save(profile);
            return profile;
        }
        profile.setId(exist.getId());
        profile.setUserId(userId);
        this.updateById(profile);
        return profile;
    }
}
