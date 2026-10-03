package com.takeout.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单 DTO（顾客/商家共用，原因可选）
 */
@Data
@Schema(description = "取消订单入参")
public class CancelOrderDTO {

    @Size(max = 100, message = "取消原因不能超过 100 字")
    @Schema(description = "取消原因")
    private String reason;
}