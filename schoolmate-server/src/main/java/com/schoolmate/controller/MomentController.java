package com.schoolmate.controller;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.dto.moment.MomentCreateDTO;
import com.schoolmate.service.MomentService;
import com.schoolmate.vo.moment.MomentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班级动态控制器。
 *
 * @author Albot
 */
@Tag(name = "班级动态", description = "班级动态 / 时间轴")
@RestController
@RequestMapping("/classes/{classId}/moments")
public class MomentController {

    @Resource
    private MomentService momentService;

    @Operation(summary = "分页查询班级动态（时间轴，按时间倒序）")
    @GetMapping
    public Result<PageResult<MomentVO>> page(
        @PathVariable Long classId,
        @RequestParam(defaultValue = "1") Long pageNum,
        @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(momentService.pageMoments(classId, pageNum, pageSize));
    }

    @Operation(summary = "发布班级动态（需为班级成员）")
    @PostMapping
    public Result<MomentVO> create(@PathVariable Long classId, @Valid @RequestBody MomentCreateDTO dto) {
        return Result.success(momentService.createMoment(classId, dto), "发布成功");
    }
}
