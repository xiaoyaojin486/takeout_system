package com.takeout.address.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增/修改 DTO
 */
@Data
@Schema(description = "收货地址入参")
public class AddressDTO {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 20, message = "收货人姓名过长")
    private String receiver;

    @NotBlank(message = "收货电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 30)
    private String province;

    @Size(max = 30)
    private String city;

    @Size(max = 30)
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址过长")
    private String detail;

    @Size(max = 10)
    @Schema(description = "标签：家 / 公司 / 学校")
    private String label;

    @Schema(description = "1 设为默认地址 / 0 普通")
    private Integer isDefault;
}