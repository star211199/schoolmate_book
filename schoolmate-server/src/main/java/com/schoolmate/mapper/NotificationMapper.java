package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知中心 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
