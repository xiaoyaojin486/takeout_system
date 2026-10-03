package com.takeout.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 店铺 Mapper：只做数据库访问，不写业务逻辑
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 累加店铺销量
     *
     * <p>只在订单完成时调用，取消链路不回退，因此这里只需一条加法语句。
     * 走 SQL 原子自增而非「查出来 +1 再写回」，避免并发完成订单时相互覆盖。</p>
     */
    @Update("UPDATE merchant SET sales = sales + #{delta} WHERE id = #{merchantId}")
    int incrSales(@Param("merchantId") Long merchantId, @Param("delta") int delta);
}