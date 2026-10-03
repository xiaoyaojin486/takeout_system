package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传配置
 */
@Data
@ConfigurationProperties(prefix = "takeout.file")
public class FileProperties {

    /** 本地存储目录 */
    private String uploadPath = "./upload";

    /** 对外访问前缀，与 WebConfig 静态资源映射保持一致 */
    private String accessPrefix = "/upload";
}