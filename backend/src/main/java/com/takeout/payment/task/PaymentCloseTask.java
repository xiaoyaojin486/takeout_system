package com.takeout.payment.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.common.enums.OrderStatusEnum;
import com.takeout.common.enums.PayStatusEnum;
import com.takeout.order.api.OrderApi;
import com.takeout.order.api.vo.OrderSimpleVO;
import com.takeout.payment.entity.Payment;
import com.takeout.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作废「订单已取消但支付单仍待支付」的支付单
 *
 * <p>场景：用户点了支付页但没付，15 分钟后订单被系统自动取消，
 * 这张支付单如果不处理，会一直显示「待支付」，用户回去看订单会困惑。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCloseTask {

    /** 支付单创建超过该时长才纳入检查，避免误伤刚下单正在支付的用户 */
    private static final int CLOSE_AFTER_MINUTES = 30;

    private final PaymentMapper paymentMapper;
    private final OrderApi orderApi;

    @Scheduled(initialDelay = 60_000, fixedDelay = 120_000)
    public void closeUnpaidPayments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(CLOSE_AFTER_MINUTES);
        List<Payment> list = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                .select(Payment::getId, Payment::getOrderId, Payment::getPayNo, Payment::getStatus)
                .eq(Payment::getStatus, PayStatusEnum.UNPAID.getCode())
                .lt(Payment::getCreateTime, deadline)
                .last("limit 200"));
        if (list.isEmpty()) {
            return;
        }

        int closed = 0;
        for (Payment payment : list) {
            OrderSimpleVO order = orderApi.getById(payment.getOrderId());
            // 订单被删除或已取消 → 这张支付单不再有意义
            if (order == null || OrderStatusEnum.CANCELED.getCode().equals(order.getStatus())) {
                Payment update = new Payment();
                update.setId(payment.getId());
                update.setStatus(PayStatusEnum.CLOSED.getCode());
                paymentMapper.updateById(update);
                closed++;
            }
        }
        if (closed > 0) {
            log.info("作废未支付的支付单 {} 笔（对应订单已取消）", closed);
        }
    }
}