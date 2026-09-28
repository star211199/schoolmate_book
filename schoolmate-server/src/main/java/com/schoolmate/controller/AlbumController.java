package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.dto.album.AlbumCreateDTO;
import com.schoolmate.service.AlbumService;
import com.schoolmate.vo.album.AlbumVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 相册控制器（班级下的相册资源）。
 *
 * @author Albot
 */
@Tag(name = "班级相册", description = "相册创建与列表")
@RestController
@RequestMapping("/classes/{classId}/albums")
public class AlbumController {

    @Resource
    private AlbumService albumService;

    @Operation(summary = "创建相册")
    @PostMapping
    public Result<AlbumVO> create(@PathVariable Long classId, @Valid @RequestBody AlbumCreateDTO dto) {
        return Result.success(albumService.createAlbum(classId, dto), "创建成功");
    }

    @Operation(summary = "查询班级相册列表")
    @GetMapping
    public Result<List<AlbumVO>> list(@PathVariable Long classId) {
        return Result.success(albumService.listAlbums(classId));
    }
}
