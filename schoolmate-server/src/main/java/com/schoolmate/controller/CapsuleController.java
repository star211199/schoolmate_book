package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.capsule.CapsuleCreateDTO;
import com.schoolmate.service.CapsuleService;
import com.schoolmate.vo.capsule.CapsuleVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 时光胶囊控制器。
 *
 * @author Albot
 */
@Tag(name = "时光胶囊", description = "写给未来的自己或全班的一封信，到点才可开启")
@RestController
@RequestMapping("/capsules")
public class CapsuleController {

    @Resource
    private CapsuleService capsuleService;

    @Operation(summary = "写一封信并封存（SELF 写给自己 / PUBLIC 写给全班，需传 classId）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CapsuleCreateDTO dto) {
        return Result.success(capsuleService.create(UserContext.getUserId(), dto), "信件已封存，静待开启");
    }

    @Operation(summary = "我的胶囊列表（含倒计时，未到点不返回内容）")
    @GetMapping("/my")
    public Result<List<CapsuleVO>> listMine() {
        return Result.success(capsuleService.listMine(UserContext.getUserId()));
    }

    @Operation(summary = "班级公开胶囊墙（需为班级成员）")
    @GetMapping("/class/{classId}")
    public Result<List<CapsuleVO>> listClassPublic(@PathVariable Long classId) {
        return Result.success(capsuleService.listClassPublic(UserContext.getUserId(), classId));
    }

    @Operation(summary = "胶囊详情（未到开启时间不返回 content，仅返回倒计时）")
    @GetMapping("/{id}")
    public Result<CapsuleVO> detail(@PathVariable Long id) {
        return Result.success(capsuleService.detail(UserContext.getUserId(), id));
    }

    @Operation(summary = "删除胶囊（仅本人且封存中，已开启不允许删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        capsuleService.delete(UserContext.getUserId(), id);
        return Result.success();
    }
}
