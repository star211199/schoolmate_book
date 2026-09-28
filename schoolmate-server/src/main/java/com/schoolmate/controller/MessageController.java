package com.schoolmate.controller;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.dto.message.MessageCreateDTO;
import com.schoolmate.service.MessageService;
import com.schoolmate.vo.message.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 留言控制器。
 *
 * @author Albot
 */
@Tag(name = "班级留言", description = "班级留言板")
@RestController
@RequestMapping("/classes/{classId}/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Operation(summary = "分页查询班级留言")
    @GetMapping
    public Result<PageResult<MessageVO>> page(
        @PathVariable Long classId,
        @RequestParam(defaultValue = "1") Long pageNum,
        @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(messageService.pageMessages(classId, pageNum, pageSize));
    }

    @Operation(summary = "发布留言（需为班级成员）")
    @PostMapping
    public Result<MessageVO> create(@PathVariable Long classId, @Valid @RequestBody MessageCreateDTO dto) {
        return Result.success(messageService.createMessage(classId, dto), "发布成功");
    }
}
