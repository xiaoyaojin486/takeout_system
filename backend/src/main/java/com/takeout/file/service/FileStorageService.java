package com.takeout.file.service;

import com.takeout.file.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务
 *
 * <p>一期本地磁盘实现；二期换 OSS 只需新增一个实现类，调用方无需改动。</p>
 */
public interface FileStorageService {

    /** 保存上传文件并返回可访问 URL */
    UploadVO upload(MultipartFile file);
}