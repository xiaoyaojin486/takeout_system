package com.takeout.address.service;

import com.takeout.address.dto.AddressDTO;
import com.takeout.address.vo.AddressVO;

import java.util.List;

/**
 * 收货地址服务
 */
public interface AddressService {

    /** 我的地址列表（默认地址排最前） */
    List<AddressVO> list();

    /** 新增地址；设为默认时自动取消原默认地址 */
    void create(AddressDTO dto);

    /** 修改地址（需归属校验） */
    void update(Long id, AddressDTO dto);

    /** 删除地址（需归属校验）；删掉默认地址后自动把另一条提为默认 */
    void delete(Long id);

    /** 设为默认地址 */
    void setDefault(Long id);
}