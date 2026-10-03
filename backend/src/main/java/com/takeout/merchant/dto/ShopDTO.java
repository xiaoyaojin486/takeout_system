package com.takeout.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺信息入参（商家编辑自己的店铺 / 入驻申请共用）
 */
@Data
@Schema(description = "店铺信息入参")
public class ShopDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 50, message = "店铺名称长度不能超过 50")
    @Schema(description = "店铺名称")
    private String name;

    @Size(max = 255, message = "店铺 logo 地址过长")
    @Schema(description = "店铺 logo URL")
    private String logo;

    @Size(max = 255, message = "店铺简介过长")
    @Schema(description = "店铺简介")
    private String description;

    @Size(max = 20, message = "联系电话过长")
    @Schema(description = "联系电话")
    private String phone;

    @Size(max = 255, message = "店铺地址过长")
    @Schema(description = "店铺地址")
    private String address;

    @NotNull(message = "起送价不能为空")
    @DecimalMin(value = "0", message = "起送价不能为负数")
    @Schema(description = "起送价")
    private BigDecimal minAmount;

    @NotNull(message = "配送费不能为空")
    @DecimalMin(value = "0", message = "配送费不能为负数")
    @Schema(description = "配送费")
    private BigDecimal deliveryFee;
}