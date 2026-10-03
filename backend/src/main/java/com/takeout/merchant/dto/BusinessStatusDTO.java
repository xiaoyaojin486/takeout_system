package com.takeout.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 营业状态入参
 */
@Data
@Schema(description = "营业状态入参")
public class BusinessStatusDTO {

    @NotNull(message = "营业状态不能为空")
    @Schema(description = "1 营业中 / 0 休息中")
    private Integer businessStatus;
}