package com.takeout.order.service;

import com.takeout.common.result.PageResult;
import com.takeout.order.dto.CreateOrderDTO;
import com.takeout.order.dto.OrderQuery;
import com.takeout.order.entity.Order;
import com.takeout.order.vo.OrderCreateVO;
import com.takeout.order.vo.OrderDetailVO;
import com.takeout.order.vo.OrderVO;

import java.util.List;

/**
 * 订单服务
 *
 * <p>
 * 三端入口都在这里，但归属校验各自独立：顾客端一律按 userId 过滤，
 * 商家端一律按「当前登录商家自己的 merchantId」过滤。
 * </p>
 */
public interface OrderService {

    // ==================== 顾客端 ====================

    /** 下单：校验 + 重算金额 + 原子扣库存 + 落订单/明细/日志 + 清购物车 */
    OrderCreateVO createOrder(CreateOrderDTO dto);

    /** 我的订单分页 */
    PageResult<OrderVO> myOrders(OrderQuery query);

    /** 我的订单详情（含状态时间轴） */
    OrderDetailVO myOrderDetail(Long orderId);

    /** 顾客取消订单（仅待支付/待接单可取消），会归还库存 */
    void cancelByCustomer(Long orderId, String reason);

    /** 顾客确认收货：配送中 → 已完成，累加销量 */
    void confirmByCustomer(Long orderId);

    // ==================== 商家端 ====================

    /** 商家订单分页 */
    PageResult<OrderVO> merchantOrders(OrderQuery query);

    /** 商家订单详情 */
    OrderDetailVO merchantOrderDetail(Long orderId);

    /** 商家接单：待接单 → 备餐中 */
    void acceptByMerchant(Long orderId);

    /** 商家拒单：待接单 → 已取消，归还库存 */
    void rejectByMerchant(Long orderId, String reason);

    /** 商家取消订单（备餐中不可抗力）：归还库存 */
    void cancelByMerchant(Long orderId, String reason);

    /** 商家出餐：备餐中 → 配送中 */
    void deliverByMerchant(Long orderId);

    /** 商家送达：配送中 → 已完成，累加销量 */
    void finishByMerchant(Long orderId);

    // ==================== 管理后台 ====================

    /** 全平台订单分页 */
    PageResult<OrderVO> adminOrders(OrderQuery query);

    /** 全平台订单详情 */
    OrderDetailVO adminOrderDetail(Long orderId);

    // ==================== 系统 ====================

    /**
     * 支付成功回调：待支付 → 待接单
     *
     * <p>
     * 由 payment 模块通过 OrderApi 调用。真实支付接入后，这里就是异步回调的落点。
     * </p>
     */
    void markPaid(Long orderId);

    /**
     * 关闭单笔超时未支付订单
     *
     * <p>
     * <b>必须由外部 Bean 调用</b>（OrderTimeoutTask），这样 @Transactional 才生效；
     * 若在本类内部循环调用自己，注解会失效，库存归还会跟着订单状态一起回滚掉。
     * </p>
     */
    void closeTimeoutOrder(Order order);

    /** 查询超时未支付的订单（只查，不开事务） */
    List<Order> listTimeoutOrders(int timeoutMinutes);
}