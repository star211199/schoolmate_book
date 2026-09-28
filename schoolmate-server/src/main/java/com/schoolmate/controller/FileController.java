package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.utils.FileUploadUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件控制器：通用文件上传（头像等）。
 *
 * @author Albot
 */
@Tag(name = "文件管理", description = "通用文件上传")
@RestController
@RequestMapping("/files")
public class FileController {

    @Resource
    private FileUploadUtil fileUploadUtil;

    @Operation(summary = "上传图片，返回访问 URL")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        String url = fileUploadUtil.upload(file);
        return Result.success(Map.of("url", url), "上传成功");
    }
}
