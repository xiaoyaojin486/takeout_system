package com.takeout.address.convert;

import com.takeout.address.api.vo.AddressSimpleVO;
import com.takeout.address.dto.AddressDTO;
import com.takeout.address.entity.Address;
import com.takeout.address.vo.AddressVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 收货地址对象转换
 */
@Mapper
public interface AddressConvert {

    AddressVO toVO(Address entity);

    Address toEntity(AddressDTO dto);

    @Mapping(target = "addressId", source = "id")
    @Mapping(target = "fullAddress", ignore = true)
    AddressSimpleVO toSimpleVO(Address entity);
}