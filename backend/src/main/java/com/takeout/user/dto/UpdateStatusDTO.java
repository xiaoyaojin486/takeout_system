package com.takeout.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/停用账号 DTO
 */
@Data
@Schema(description = "修改账号状态入参")
public class UpdateStatusDTO {

    @NotNull(message = "状态不能为空")
    @Schema(description = "1 正常 / 0 停用")
    private Integer status;
}