package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.common.PageResult;
import com.schoolmate.dto.moment.MomentCreateDTO;
import com.schoolmate.entity.Moment;
import com.schoolmate.vo.moment.CommentVO;
import com.schoolmate.vo.moment.MomentVO;

import java.util.List;

/**
 * 班级动态 / 时间轴服务接口。
 *
 * @author Albot
 */
public interface MomentService extends IService<Moment> {

    /**
     * 分页查询班级动态（时间轴，按时间倒序）。
     */
    PageResult<MomentVO> pageMoments(Long classId, Long pageNum, Long pageSize);

    /**
     * 发布动态（需为班级成员）。
     */
    MomentVO createMoment(Long classId, MomentCreateDTO dto);

    /**
     * 修改动态（发布人或管理员）。
     */
    MomentVO updateMoment(Long id, MomentCreateDTO dto);

    /**
     * 删除动态（发布人或管理员）。
     */
    void deleteMoment(Long id);

    /**
     * 查询动态的评论列表。
     */
    List<CommentVO> listComments(Long momentId);

    /**
     * 发表评论。
     */
    CommentVO createComment(Long momentId, com.schoolmate.dto.moment.CommentCreateDTO dto);

    /**
     * 删除评论（评论人或管理员）。
     */
    void deleteComment(Long id);
}
