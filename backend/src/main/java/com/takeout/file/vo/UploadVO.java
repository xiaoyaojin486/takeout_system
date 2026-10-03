package com.takeout.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果（返回前端）
 */
@Data
@Schema(description = "文件上传结果")
public class UploadVO implements Serializable {

    @Schema(description = "可访问的 URL，前端可直接用于 <img src>")
    private String url;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "文件大小（字节）")
    private Long size;
}