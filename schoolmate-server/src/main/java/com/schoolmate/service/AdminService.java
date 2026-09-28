package com.schoolmate.service;

import com.schoolmate.vo.admin.AuditVO;
import com.schoolmate.vo.admin.StatsVO;

import java.util.List;

/**
 * 后台管理服务接口。
 *
 * @author Albot
 */
public interface AdminService {

    /**
     * 仪表盘统计。
     */
    StatsVO getStats();

    /**
     * 查询待审核内容列表。
     *
     * @param type 内容类型 MESSAGE / MOMENT / PHOTO，为空表示全部
     */
    List<AuditVO> listPendingAudits(String type);

    /**
     * 审核内容（通过或驳回）。
     *
     * @param type    内容类型
     * @param id      内容ID
     * @param passed  是否通过
     */
    void audit(String type, Long id, boolean passed);
}
