package com.takeout.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发起支付 DTO
 */
@Data
@Schema(description = "支付入参")
public class PayDTO {

    @Schema(description = "1 模拟支付（一期唯一实现）/ 2 微信 / 3 支付宝", example = "1")
    private Integer payType;
}