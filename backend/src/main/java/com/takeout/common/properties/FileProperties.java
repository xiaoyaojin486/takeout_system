package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传配置
 *
 * <p>
 * 存储实现由 {@code storageType} 决定：
 * </p>
 * <ul>
 * <li>{@code local}：写本地磁盘，开发默认，无需任何外部依赖；</li>
 * <li>{@code minio}：写 MinIO 对象存储，多实例部署必须（否则 A 实例上传的文件 B 实例读不到）。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "takeout.file")
public class FileProperties {

    /** 存储实现：local(本地磁盘) / minio(对象存储)，默认 local */
    private String storageType = "local";

    /** 本地存储目录（storageType=local 时生效） */
    private String uploadPath = "./upload";

    /** 本地存储对外访问前缀，与 WebConfig 静态资源映射保持一致 */
    private String accessPrefix = "/upload";

    /** MinIO 对象存储配置（storageType=minio 时生效） */
    private Minio minio = new Minio();

    @Data
    public static class Minio {

        /** 服务地址，容器内为 http://minio:9000 */
        private String endpoint = "http://localhost:9000";

        /** 存储桶名 */
        private String bucket = "takeout";

        /** 访问密钥（即 MinIO 的 root 用户） */
        private String accessKey;

        /** 密钥（即 MinIO 的 root 密码） */
        private String secretKey;

        /** 对外访问前缀：由 Nginx 把 /files/ 反代到 MinIO 的桶，前端 <img> 直接引用 */
        private String publicPrefix = "/files";
    }
}