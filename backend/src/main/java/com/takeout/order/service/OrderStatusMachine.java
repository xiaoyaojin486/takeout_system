package com.takeout.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.common.enums.OrderOperatorEnum;
import com.takeout.common.enums.OrderStatusEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.order.entity.Order;
import com.takeout.order.entity.OrderStatusLog;
import com.takeout.order.mapper.OrderMapper;
import com.takeout.order.mapper.OrderStatusLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单状态机
 *
 * <p>
 * <b>存在的意义</b>：订单状态是最容易写乱的地方。如果把「判断能不能改」散落在
 * 接单/拒单/取消/确认收货等十几个方法里，早晚会出现某个入口漏判、
 * 把已取消的订单改成已完成的低级错误。
 * </p>
 *
 * <p>
 * 因此所有状态变更<b>必须</b>经过 {@link #transfer}：合法性来自
 * {@link OrderStatusEnum#canTransfer} 的流转表，改状态与写日志在同一次调用里完成，
 * 不存在「改了状态但没留痕」的可能。
 * </p>
 *
 * <p>
 * 注意：本类只负责「状态」这一件事，库存归还、销量累加、退款等副作用
 * 由调用方（OrderService）负责，保持职责单一。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusMachine {

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    /**
     * 执行状态流转
     *
     * @param order      当前订单（方法内会把它的 status 同步成新值，便于调用方继续用）
     * @param target     目标状态
     * @param operator   操作方
     * @param operatorId 操作人 id（系统操作传 null）
     * @param remark     备注（拒单原因 / 取消原因）
     */
    public void transfer(Order order, OrderStatusEnum target, OrderOperatorEnum operator,
            Long operatorId, String remark) {
        Integer from = order.getStatus();
        if (!OrderStatusEnum.canTransfer(from, target.getCode())) {
            OrderStatusEnum current = OrderStatusEnum.of(from);
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, String.format(
                    "订单当前状态为「%s」，只能流转到：%s",
                    current == null ? "未知" : current.getDesc(),
                    OrderStatusEnum.allowedTargets(from)));
        }

        LocalDateTime now = LocalDateTime.now();
        Order update = new Order();
        update.setStatus(target.getCode());
        // 各节点时间只在进入对应状态时写一次，后续流转不覆盖
        switch (target) {
            case PENDING_ACCEPT -> update.setPayTime(now);
            case PREPARING -> update.setAcceptTime(now);
            case COMPLETED -> update.setFinishTime(now);
            case CANCELED -> {
                update.setCancelTime(now);
                update.setCancelReason(remark);
            }
            default -> {
                // REFUNDED 等状态的时间字段在二期实现时补充
            }
        }

        // 【并发保护】把「原状态」也作为 WHERE 条件（乐观锁）。
        //
        // 为什么不能只按 id 更新：上面的 canTransfer 校验的是「内存里读到的状态」，
        // 从读取到写入之间存在时间窗（TOCTOU）。两个请求同时读到 status=10 时都会通过校验，
        // 而调用方在此之前已经执行了还库存，最终库存会被归还两遍 → 库存虚高 → 可能超卖。
        //
        // 加上 eq(status, from) 后，后到的那个请求影响行数为 0，直接被拒；
        // 判断与更新在一次原子操作里完成，时间窗被消除。
        int affected = orderMapper.update(update, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, from));
        if (affected == 0) {
            log.warn("订单状态并发冲突，已放弃本次流转 orderId={} from={} to={}",
                    order.getId(), from, target.getCode());
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "订单状态已被其他操作变更，请刷新后重试");
        }
        order.setStatus(target.getCode());

        writeLog(order.getId(), from, target.getCode(), operator, operatorId, remark);
        log.info("订单状态流转 orderId={} orderNo={} {} -> {}", order.getId(), order.getOrderNo(),
                from, target.getCode());
    }

    /**
     * 记录下单初始状态（0 → 10）
     *
     * <p>
     * 下单没有「原状态」，走独立方法而不是硬塞一个 0 进流转表，
     * 否则流转表会多出一堆无意义的 {0 -> xx} 记录。
     * </p>
     */
    public void logCreated(Long orderId, Long operatorId, String remark) {
        writeLog(orderId, 0, OrderStatusEnum.PENDING_PAYMENT.getCode(),
                OrderOperatorEnum.CUSTOMER, operatorId, remark);
    }

    private void writeLog(Long orderId, Integer fromStatus, Integer toStatus,
            OrderOperatorEnum operator, Long operatorId, String remark) {
        OrderStatusLog entity = new OrderStatusLog();
        entity.setOrderId(orderId);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setOperatorType(operator.getCode());
        entity.setOperatorId(operatorId);
        entity.setRemark(remark);
        entity.setCreateTime(LocalDateTime.now());
        orderStatusLogMapper.insert(entity);
    }
}