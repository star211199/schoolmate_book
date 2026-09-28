package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.dto.moment.CommentCreateDTO;
import com.schoolmate.dto.moment.MomentCreateDTO;
import com.schoolmate.service.MomentService;
import com.schoolmate.vo.moment.CommentVO;
import com.schoolmate.vo.moment.MomentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 动态与评论资源控制器。
 *
 * @author Albot
 */
@Tag(name = "班级动态", description = "动态修改删除、评论")
@RestController
@RequestMapping("/moments")
public class MomentResourceController {

    @Resource
    private MomentService momentService;

    @Operation(summary = "修改动态")
    @PutMapping("/{id}")
    public Result<MomentVO> update(@PathVariable Long id, @Valid @RequestBody MomentCreateDTO dto) {
        return Result.success(momentService.updateMoment(id, dto), "修改成功");
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        momentService.deleteMoment(id);
        return Result.success(null, "删除成功");
    }

    @Operation(summary = "查询动态评论列表")
    @GetMapping("/{id}/comments")
    public Result<List<CommentVO>> listComments(@PathVariable Long id) {
        return Result.success(momentService.listComments(id));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{id}/comments")
    public Result<CommentVO> createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateDTO dto) {
        return Result.success(momentService.createComment(id, dto), "评论成功");
    }
}
