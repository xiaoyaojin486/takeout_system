package com.takeout.address.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址简要信息（跨模块契约 VO）
 *
 * <p>订单模块下单时用它生成地址快照。</p>
 */
@Data
@Builder
public class AddressSimpleVO implements Serializable {

    private Long addressId;

    private Long userId;

    private String receiver;

    private String phone;

    /** 省市区 + 详细地址拼接后的完整地址 */
    private String fullAddress;
}