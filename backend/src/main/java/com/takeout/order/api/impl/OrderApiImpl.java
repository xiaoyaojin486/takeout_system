package com.takeout.order.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.common.enums.OrderStatusEnum;
import com.takeout.order.api.OrderApi;
import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.order.api.vo.MerchantOrderStatVO;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.order.api.vo.OrderSimpleVO;
import com.takeout.order.api.vo.PlatformOrderStatVO;
import com.takeout.order.convert.OrderConvert;
import com.takeout.order.entity.Order;
import com.takeout.order.mapper.OrderMapper;
import com.takeout.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单契约实现
 *
 * <p>
 * 统计口径统一为「已完成订单」：{@code status = 50}，时间按 finish_time 归属。
 * 这样「营业额」「订单数」「菜品销量」三者永远能对上账。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderApiImpl implements OrderApi {

    private final OrderMapper orderMapper;
    private final OrderConvert orderConvert;
    private final OrderService orderService;

    @Override
    public OrderSimpleVO getByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .select(Order::getId, Order::getOrderNo, Order::getUserId, Order::getMerchantId,
                        Order::getPayAmount, Order::getStatus, Order::getReceiver,
                        Order::getReceiverPhone, Order::getReceiverAddress)
                .eq(Order::getOrderNo, orderNo)
                .last("limit 1"));
        return order == null ? null : orderConvert.toSimpleVO(order);
    }

    @Override
    public OrderSimpleVO getById(Long orderId) {
        if (orderId == null) {
            return null;
        }
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .select(Order::getId, Order::getOrderNo, Order::getUserId, Order::getMerchantId,
                        Order::getPayAmount, Order::getStatus, Order::getReceiver,
                        Order::getReceiverPhone, Order::getReceiverAddress)
                .eq(Order::getId, orderId));
        return order == null ? null : orderConvert.toSimpleVO(order);
    }

    @Override
    public void markPaid(Long orderId) {
        orderService.markPaid(orderId);
    }

    @Override
    public MerchantOrderStatVO statMerchant(Long merchantId, LocalDate from, LocalDate to) {
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.plusDays(1).atStartOfDay();

        MerchantOrderStatVO vo = new MerchantOrderStatVO();
        vo.setOrderCount(orderMapper.countCompletedByMerchant(merchantId, fromTime, toTime));
        vo.setSalesAmount(orderMapper.sumAmountByMerchant(merchantId, fromTime, toTime));
        // 待接单/待支付是实时指标，不受查询区间影响
        vo.setPendingAcceptCount(orderMapper.countByMerchantAndStatus(
                merchantId, OrderStatusEnum.PENDING_ACCEPT.getCode()));
        vo.setPendingPaymentCount(orderMapper.countByMerchantAndStatus(
                merchantId, OrderStatusEnum.PENDING_PAYMENT.getCode()));
        return vo;
    }

    @Override
    public List<DailyStatVO> statMerchantTrend(Long merchantId, LocalDate from, LocalDate to) {
        return orderMapper.statDailyByMerchant(merchantId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    @Override
    public List<DishRankVO> statHotDish(Long merchantId, LocalDate from, LocalDate to, int limit) {
        return orderMapper.statHotDishByMerchant(merchantId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), limit);
    }

    @Override
    public PlatformOrderStatVO statPlatform(LocalDate from, LocalDate to) {
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.plusDays(1).atStartOfDay();

        PlatformOrderStatVO vo = new PlatformOrderStatVO();
        vo.setOrderCount(orderMapper.countCompletedPlatform(fromTime, toTime));
        vo.setSalesAmount(orderMapper.sumAmountPlatform(fromTime, toTime));
        vo.setCanceledCount(orderMapper.countCanceledPlatform(fromTime, toTime));
        return vo;
    }

    @Override
    public List<MerchantRankVO> statMerchantRank(LocalDate from, LocalDate to, int limit) {
        return orderMapper.statMerchantRank(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), limit);
    }
}