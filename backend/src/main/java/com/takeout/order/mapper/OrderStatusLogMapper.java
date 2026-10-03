package com.takeout.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.order.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态日志 Mapper
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}