package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.service.AlbumService;
import com.schoolmate.vo.album.AlbumVO;
import com.schoolmate.vo.album.PhotoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 相册/照片资源控制器。
 *
 * @author Albot
 */
@Tag(name = "班级相册", description = "相册详情、照片上传与列表")
@RestController
@RequestMapping("/albums")
public class AlbumResourceController {

    @Resource
    private AlbumService albumService;

    @Operation(summary = "查询相册详情")
    @GetMapping("/{id}")
    public Result<AlbumVO> detail(@PathVariable Long id) {
        return Result.success(albumService.getAlbum(id));
    }

    @Operation(summary = "删除相册")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        albumService.deleteAlbum(id);
        return Result.success(null, "删除成功");
    }

    @Operation(summary = "上传照片到相册")
    @PostMapping("/{id}/photos")
    public Result<PhotoVO> uploadPhoto(@PathVariable Long id,
                                       @RequestPart("file") MultipartFile file,
                                       @RequestParam(required = false) String description) {
        return Result.success(albumService.uploadPhoto(id, file, description), "上传成功");
    }

    @Operation(summary = "查询相册照片列表")
    @GetMapping("/{id}/photos")
    public Result<List<PhotoVO>> listPhotos(@PathVariable Long id) {
        return Result.success(albumService.listPhotos(id));
    }
}
