package com.takeout.address.api;

import com.takeout.address.api.vo.AddressSimpleVO;

/**
 * 收货地址模块对外契约
 */
public interface AddressApi {

    /**
     * 按 id + 用户 id 查询
     *
     * <p><b>强制带 userId 参数</b>：订单模块下单时只能取「自己的」地址，
     * 把归属校验下沉到契约层，调用方想越权都做不到。</p>
     *
     * @return null 表示地址不存在或不属于该用户
     */
    AddressSimpleVO findByIdAndUserId(Long addressId, Long userId);

    /** 取用户的默认地址（没有默认地址时返回 null） */
    AddressSimpleVO findDefaultByUserId(Long userId);
}