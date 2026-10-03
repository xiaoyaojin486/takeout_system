package com.takeout.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.address.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 收货地址 Mapper
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    /**
     * 清除某用户的默认地址标记
     *
     * <p>只更新仍为默认的那一行，避免无谓写库；
     * 必须在「设置新默认地址」之前调用，否则会出现两个默认地址。</p>
     */
    @Update("UPDATE address SET is_default = 0 WHERE user_id = #{userId} AND is_default = 1 AND del_flag = 0")
    int clearDefault(@Param("userId") Long userId);
}