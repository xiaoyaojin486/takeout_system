package com.takeout.file.service.impl;

import com.takeout.common.exception.BusinessException;
import com.takeout.common.properties.FileProperties;
import com.takeout.common.result.ResultCode;
import com.takeout.file.service.FileStorageService;
import com.takeout.file.vo.UploadVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO 对象存储实现（生产多实例用）
 *
 * <p>启用方式：{@code FILE_STORAGE_TYPE=minio}，并配置 MINIO_ENDPOINT / MINIO_BUCKET /
 * MINIO_ACCESS_KEY / MINIO_SECRET_KEY。</p>
 *
 * <p>对象键与本地实现保持一致：{@code yyyy/MM/dd/{uuid}.{ext}}，便于两种实现互相迁移。
 * 返回的 URL 是相对路径 {@code {publicPrefix}/{objectName}}，由 Nginx 反代到 MinIO，
 * 前端无需知道 MinIO 的真实地址。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "takeout.file", name = "storage-type", havingValue = "minio")
public class MinioFileStorageServiceImpl implements FileStorageService {

    /** 与本地实现保持同一套校验规则：只放行图片，且不超过 5MB */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** MinIO 与应用同时启动，服务端可能稍晚就绪，这里最多重试 30 次 × 2s */
    private static final int INIT_MAX_ATTEMPTS = 30;
    private static final long INIT_RETRY_INTERVAL_MS = 2000L;

    private final FileProperties fileProperties;

    private MinioClient minioClient;

    /**
     * 启动时建立客户端并确保存储桶存在、为公共读。
     *
     * <p>桶与策略由应用自建，免去部署时手工执行 mc 命令；重试是为了容忍
     * 「MinIO 容器比后端容器晚就绪」的启动顺序问题。</p>
     */
    @PostConstruct
    void initBucket() {
        FileProperties.Minio cfg = fileProperties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(cfg.getEndpoint())
                .credentials(cfg.getAccessKey(), cfg.getSecretKey())
                .build();

        for (int attempt = 1; attempt <= INIT_MAX_ATTEMPTS; attempt++) {
            try {
                ensureBucket(cfg.getBucket());
                log.info("MinIO 初始化完成 endpoint={} bucket={}", cfg.getEndpoint(), cfg.getBucket());
                return;
            } catch (Exception e) {
                if (attempt == INIT_MAX_ATTEMPTS) {
                    // 不抛异常中断启动：应用其余功能仍可用，仅文件上传受影响，日志已明确指向原因
                    log.error("MinIO 初始化失败（已重试 {} 次），图片上传将不可用，请检查 MINIO_* 配置与容器状态",
                            INIT_MAX_ATTEMPTS, e);
                    return;
                }
                log.warn("MinIO 尚未就绪，{}ms 后重试（第 {}/{} 次）：{}",
                        INIT_RETRY_INTERVAL_MS, attempt, INIT_MAX_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(INIT_RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO 存储桶已创建：{}", bucket);
        }
        // 设为公共读：图片/头像要能被浏览器匿名加载（经 Nginx /files/ 反代到 MinIO）
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(publicReadPolicy(bucket))
                .build());
    }

    /** 只授予 GetObject，不给 ListBucket/写权限，避免桶被匿名枚举或篡改 */
    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }

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

        if (minioClient == null) {
            log.error("MinIO 客户端未初始化，上传被拒绝");
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        String dateDir = LocalDate.now().format(DATE_DIR_FORMATTER);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String objectName = dateDir + "/" + fileName;
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType() : "application/octet-stream";

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(fileProperties.getMinio().getBucket())
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            // 网络不通 / 桶不存在 / 密钥错误等：统一包装，不把 SDK 堆栈暴露给前端
            log.error("MinIO 上传失败 originalFilename={} object={}", originalFilename, objectName, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        String url = fileProperties.getMinio().getPublicPrefix() + "/" + objectName;
        UploadVO vo = new UploadVO();
        vo.setUrl(url);
        vo.setName(originalFilename);
        vo.setSize(file.getSize());

        log.info("文件上传成功(MinIO) url={} size={}", url, file.getSize());
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