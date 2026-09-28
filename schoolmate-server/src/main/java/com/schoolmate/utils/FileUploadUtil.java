package com.schoolmate.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传工具：本地磁盘存储 + 扩展名白名单校验。
 *
 * @author Albot
 */
@Slf4j
@Component
public class FileUploadUtil {

    /** 允许的扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Value("${schoolmate.upload.base-dir}")
    private String baseDir;

    /**
     * 保存上传文件，返回可访问的相对 URL。
     *
     * @param file 上传的文件
     * @return 访问 URL，如 /upload/2026/09/28/xxx.jpg
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅允许：" + String.join("、", ALLOWED_EXTENSIONS));
        }

        String datePath = LocalDate.now().format(DATE_PATTERN);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = Paths.get(baseDir, datePath.split("/"));
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalName, e);
            throw new IllegalStateException("文件上传失败，请稍后重试");
        }

        return "/upload/" + datePath + "/" + fileName;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 解析 JSON 格式的图片 URL 数组。
     */
    public static List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
    }
}
