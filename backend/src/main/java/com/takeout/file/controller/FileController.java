package com.takeout.file.controller;

import com.takeout.common.result.Result;
import com.takeout.file.service.FileStorageService;
import com.takeout.file.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 *
 * <p>{@code /file/upload} 不在鉴权白名单内，天然要求携带有效 accessToken；
 * 三端登录用户均可上传，不做端级限制。</p>
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "文件上传")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "图片上传", description = "支持 jpg/jpeg/png/gif/webp，单文件不超过 5MB，返回可直接访问的 URL")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.upload(file));
    }
}