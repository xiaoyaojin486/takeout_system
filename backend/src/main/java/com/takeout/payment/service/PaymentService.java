package com.takeout.payment.service;

import com.takeout.payment.vo.PaymentVO;

/**
 * 支付服务
 *
 * <p><b>模块依赖方向</b>：payment → order-api（单向）。
 * 订单模块不反向依赖支付模块，因此「下单」不会创建支付单，
 * 支付单在首次发起支付时才按订单金额生成（见 createIfAbsent）。</p>
 */
public interface PaymentService {

    /**
     * 发起支付（一期为模拟支付：直接把支付单置为已支付并通知订单模块）
     *
     * @param orderNo 订单号
     * @param payType 支付方式，为空默认模拟支付
     */
    PaymentVO pay(String orderNo, Integer payType);

    /** 按订单号查支付单（不存在返回 null） */
    PaymentVO getByOrderNo(String orderNo);
}