package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 留言资源控制器（按资源 ID 操作）。
 *
 * @author Albot
 */
@Tag(name = "班级留言", description = "删除留言")
@RestController
@RequestMapping("/messages")
public class MessageResourceController {

    @Resource
    private MessageService messageService;

    @Operation(summary = "删除留言（本人或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return Result.success(null, "删除成功");
    }
}
