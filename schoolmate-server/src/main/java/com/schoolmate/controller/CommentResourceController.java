package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.service.MomentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论资源控制器。
 *
 * @author Albot
 */
@Tag(name = "班级动态", description = "删除评论")
@RestController
@RequestMapping("/comments")
public class CommentResourceController {

    @Resource
    private MomentService momentService;

    @Operation(summary = "删除评论（评论人或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        momentService.deleteComment(id);
        return Result.success(null, "删除成功");
    }
}
