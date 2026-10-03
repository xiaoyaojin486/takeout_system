package com.takeout.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}