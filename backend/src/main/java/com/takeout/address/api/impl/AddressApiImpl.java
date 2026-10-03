package com.takeout.address.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.address.api.AddressApi;
import com.takeout.address.api.vo.AddressSimpleVO;
import com.takeout.address.convert.AddressConvert;
import com.takeout.address.entity.Address;
import com.takeout.address.mapper.AddressMapper;
import com.takeout.address.service.impl.AddressServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 收货地址契约实现
 *
 * <p>查询条件里强制带 userId，调用方无法绕过归属校验。</p>
 */
@Service
@RequiredArgsConstructor
public class AddressApiImpl implements AddressApi {

    private final AddressMapper addressMapper;
    private final AddressConvert addressConvert;

    @Override
    public AddressSimpleVO findByIdAndUserId(Long addressId, Long userId) {
        if (addressId == null || userId == null) {
            return null;
        }
        Address address = addressMapper.selectOne(Wrappers.<Address>lambdaQuery()
                .select(Address::getId, Address::getUserId, Address::getReceiver, Address::getPhone,
                        Address::getProvince, Address::getCity, Address::getDistrict, Address::getDetail)
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId));
        return toSimpleVO(address);
    }

    @Override
    public AddressSimpleVO findDefaultByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        Address address = addressMapper.selectOne(Wrappers.<Address>lambdaQuery()
                .select(Address::getId, Address::getUserId, Address::getReceiver, Address::getPhone,
                        Address::getProvince, Address::getCity, Address::getDistrict, Address::getDetail)
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .last("limit 1"));
        return toSimpleVO(address);
    }

    private AddressSimpleVO toSimpleVO(Address address) {
        if (address == null) {
            return null;
        }
        AddressSimpleVO vo = addressConvert.toSimpleVO(address);
        vo.setFullAddress(AddressServiceImpl.joinFullAddress(address));
        return vo;
    }
}