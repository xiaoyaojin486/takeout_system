package com.takeout.address.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址 VO
 */
@Data
@Schema(description = "收货地址")
public class AddressVO implements Serializable {

    private Long id;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private String label;

    @Schema(description = "1 默认地址 / 0 普通")
    private Integer isDefault;

    /** 省市区 + 详细地址拼接结果，前端直接展示，避免每处都自己拼 */
    private String fullAddress;
}