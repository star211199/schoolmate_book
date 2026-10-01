package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.dto.capsule.CapsuleCreateDTO;
import com.schoolmate.entity.TimeCapsule;
import com.schoolmate.vo.capsule.CapsuleVO;

import java.util.List;

/**
 * 时光胶囊服务：写给未来的自己或全班的一封信，到点才可开启。
 *
 * @author Albot
 */
public interface CapsuleService extends IService<TimeCapsule> {

    /** 写一封信并封存（PUBLIC 类型需为班级成员） */
    Long create(Long userId, CapsuleCreateDTO dto);

    /** 我的胶囊列表（含倒计时，未到点不返回内容） */
    List<CapsuleVO> listMine(Long userId);

    /** 班级公开胶囊墙（需为班级成员） */
    List<CapsuleVO> listClassPublic(Long userId, Long classId);

    /**
     * 胶囊详情。
     *
     * <p>SELF 类型仅写信人可见；PUBLIC 类型班级成员可见；
     * 未到开启时间一律不返回 content。
     */
    CapsuleVO detail(Long userId, Long id);

    /** 删除胶囊（仅本人且封存中，已开启的信不允许删除） */
    void delete(Long userId, Long id);

    /** 定时任务：把到点的胶囊置为 OPENED，并给写信人发到期通知 */
    void openDueCapsules();
}
