package com.takeout.file.service.impl;

import com.takeout.common.exception.BusinessException;
import com.takeout.common.properties.FileProperties;
import com.takeout.common.result.ResultCode;
import com.takeout.file.service.FileStorageService;
import com.takeout.file.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘文件存储实现
 *
 * <p>存储路径：{@code {uploadPath}/yyyy/MM/dd/{uuid}.{ext}}，
 * 按日期分片避免单目录堆积海量文件；文件名用 UUID 重命名，防止同名互相覆盖。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    /** 仅允许图片扩展名：既限定了业务用途，也避免上传可执行脚本 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileProperties fileProperties;

    @Override
    public UploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_ERROR);
        }

        String dateDir = LocalDate.now().format(DATE_DIR_FORMATTER);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = Paths.get(fileProperties.getUploadPath(), dateDir).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            // 磁盘满 / 权限不足等系统级失败：包装成统一的 FILE_UPLOAD_ERROR，避免把堆栈暴露给前端
            log.error("文件上传失败 originalFilename={}", originalFilename, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        String url = fileProperties.getAccessPrefix() + "/" + dateDir + "/" + fileName;
        UploadVO vo = new UploadVO();
        vo.setUrl(url);
        vo.setName(originalFilename);
        vo.setSize(file.getSize());

        log.info("文件上传成功 url={} size={}", url, file.getSize());
        return vo;
    }

    /** 取扩展名并统一转小写，大小写混写（如 .JPG）也能正确放行 */
    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }
}