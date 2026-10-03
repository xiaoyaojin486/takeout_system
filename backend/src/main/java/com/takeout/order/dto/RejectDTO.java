package com.takeout.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家拒单 DTO
 */
@Data
@Schema(description = "拒单入参")
public class RejectDTO {

    @NotBlank(message = "请填写拒单原因")
    @Size(max = 100, message = "拒单原因不能超过 100 字")
    @Schema(description = "拒单原因，会展示给顾客")
    private String reason;
}