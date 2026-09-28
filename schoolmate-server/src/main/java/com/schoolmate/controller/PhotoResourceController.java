package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 照片资源控制器。
 *
 * @author Albot
 */
@Tag(name = "班级相册", description = "删除照片")
@RestController
@RequestMapping("/photos")
public class PhotoResourceController {

    @Resource
    private AlbumService albumService;

    @Operation(summary = "删除照片（上传者或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        albumService.deletePhoto(id);
        return Result.success(null, "删除成功");
    }
}
