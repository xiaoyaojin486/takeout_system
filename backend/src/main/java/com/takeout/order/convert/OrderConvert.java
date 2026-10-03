package com.takeout.order.convert;

import com.takeout.order.api.vo.OrderSimpleVO;
import com.takeout.order.entity.Order;
import com.takeout.order.entity.OrderItem;
import com.takeout.order.entity.OrderStatusLog;
import com.takeout.order.vo.OrderItemVO;
import com.takeout.order.vo.OrderStatusLogVO;
import com.takeout.order.vo.OrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订单对象转换
 */
@Mapper
public interface OrderConvert {

    /** Entity → 列表/详情 VO（状态描述、店铺名、明细由 Service 另行补全） */
    @Mapping(target = "statusDesc", ignore = true)
    @Mapping(target = "merchantName", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    OrderVO toVO(Order entity);

    OrderItemVO toItemVO(OrderItem entity);

    List<OrderItemVO> toItemVOList(List<OrderItem> entities);

    @Mapping(target = "fromStatusDesc", ignore = true)
    @Mapping(target = "toStatusDesc", ignore = true)
    @Mapping(target = "operatorTypeDesc", ignore = true)
    OrderStatusLogVO toLogVO(OrderStatusLog entity);

    List<OrderStatusLogVO> toLogVOList(List<OrderStatusLog> entities);

    /** Entity → 跨模块契约 VO */
    @Mapping(target = "orderId", source = "id")
    OrderSimpleVO toSimpleVO(Order entity);
}