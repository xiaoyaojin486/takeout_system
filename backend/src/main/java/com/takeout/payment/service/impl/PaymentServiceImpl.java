package com.takeout.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.OrderStatusEnum;
import com.takeout.common.enums.PayStatusEnum;
import com.takeout.common.enums.PayTypeEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.exception.ForbiddenException;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.OrderNoGenerator;
import com.takeout.order.api.OrderApi;
import com.takeout.order.api.vo.OrderSimpleVO;
import com.takeout.payment.entity.Payment;
import com.takeout.payment.mapper.PaymentMapper;
import com.takeout.payment.service.PaymentService;
import com.takeout.payment.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付服务实现（模拟支付）
 *
 * <p><b>真实支付怎么接</b>：把 {@link #pay} 里「标记已支付 + 通知订单模块」这段逻辑
 * 原样搬进异步回调接口即可，其余校验（归属、订单状态、金额一致性）完全复用。</p>
 *
 * <p><b>金额一致性</b>：支付单金额取自订单的 pay_amount，不接受前端传金额，
 * 否则用户可以「1 分钱买 100 元的菜」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderApi orderApi;
    private final OrderNoGenerator orderNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO pay(String orderNo, Integer payType) {
        Long userId = UserContextHolder.getUserId();

        OrderSimpleVO order = orderApi.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        // 只能支付自己的订单
        if (!userId.equals(order.getUserId())) {
            log.warn("越权支付 userId={} orderNo={} owner={}", userId, orderNo, order.getUserId());
            throw new ForbiddenException(ResultCode.ORDER_NOT_OWNER);
        }
        // 订单状态是支付的唯一闸口：已支付/已取消/已完成都进不来
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
            OrderStatusEnum status = OrderStatusEnum.of(order.getStatus());
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "订单当前状态为「" + (status == null ? "未知" : status.getDesc()) + "」，无法支付");
        }

        Payment payment = createIfAbsent(order, payType);

        // 重复支付直接返回已有结果，保证接口幂等
        if (PayStatusEnum.PAID.getCode().equals(payment.getStatus())) {
            return toVO(payment);
        }
        if (!PayStatusEnum.UNPAID.getCode().equals(payment.getStatus())) {
            throw new BusinessException(ResultCode.PAYMENT_STATUS_ERROR);
        }

        // ① 支付单置已支付（模拟：直接成功；真实场景由第三方回调驱动）
        Payment update = new Payment();
        update.setId(payment.getId());
        update.setStatus(PayStatusEnum.PAID.getCode());
        update.setTradeNo(orderNoGenerator.mockTradeNo());
        update.setPayTime(LocalDateTime.now());
        paymentMapper.updateById(update);

        // ② 通知订单模块推进状态：10 → 20（与上一步同一事务，不会出现"钱付了单没动"）
        orderApi.markPaid(order.getOrderId());

        log.info("支付成功 orderNo={} payNo={} amount={}", orderNo, payment.getPayNo(), payment.getAmount());

        update.setPayNo(payment.getPayNo());
        update.setAmount(payment.getAmount());
        update.setOrderId(order.getOrderId());
        update.setPayType(payment.getPayType());
        return toVO(update);
    }

    @Override
    public PaymentVO getByOrderNo(String orderNo) {
        Long userId = UserContextHolder.getUserId();
        OrderSimpleVO order = orderApi.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException(ResultCode.ORDER_NOT_OWNER);
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .select(Payment::getId, Payment::getPayNo, Payment::getOrderId, Payment::getAmount,
                        Payment::getPayType, Payment::getStatus, Payment::getTradeNo, Payment::getPayTime)
                .eq(Payment::getOrderId, order.getOrderId())
                .last("limit 1"));
        return payment == null ? null : toVO(payment);
    }

    /** 支付单不存在则按订单实付金额生成（金额取订单侧，前端不可干预） */
    private Payment createIfAbsent(OrderSimpleVO order, Integer payType) {
        Payment exist = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .select(Payment::getId, Payment::getPayNo, Payment::getOrderId, Payment::getAmount,
                        Payment::getPayType, Payment::getStatus, Payment::getTradeNo, Payment::getPayTime)
                .eq(Payment::getOrderId, order.getOrderId())
                .last("limit 1"));
        if (exist != null) {
            return exist;
        }

        Payment entity = new Payment();
        entity.setPayNo(orderNoGenerator.nextPayNo());
        entity.setOrderId(order.getOrderId());
        entity.setUserId(order.getUserId());
        entity.setAmount(order.getPayAmount());
        entity.setPayType(payType == null ? PayTypeEnum.SIMULATE.getCode() : payType);
        entity.setStatus(PayStatusEnum.UNPAID.getCode());
        paymentMapper.insert(entity);
        return entity;
    }

    private PaymentVO toVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setPayNo(payment.getPayNo());
        vo.setOrderId(payment.getOrderId());
        vo.setAmount(payment.getAmount());
        vo.setPayType(payment.getPayType());
        vo.setStatus(payment.getStatus());
        vo.setTradeNo(payment.getTradeNo());
        vo.setPayTime(payment.getPayTime());

        PayTypeEnum type = PayTypeEnum.of(payment.getPayType());
        vo.setPayTypeDesc(type == null ? null : type.getDesc());
        PayStatusEnum status = PayStatusEnum.of(payment.getStatus());
        vo.setStatusDesc(status == null ? null : status.getDesc());
        return vo;
    }
}