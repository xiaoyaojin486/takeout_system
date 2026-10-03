package com.takeout.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺审核入参（管理后台）
 */
@Data
@Schema(description = "店铺审核入参")
public class AuditDTO {

    @NotNull(message = "审核状态不能为空")
    @Schema(description = "1 已通过 / 2 已驳回")
    private Integer auditStatus;

    @Size(max = 255, message = "审核意见过长")
    @Schema(description = "审核意见（驳回时建议填写）")
    private String auditRemark;
}