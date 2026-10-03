package com.takeout.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单状态机：合法流转关系集中定义在此，禁止在 Service 里散落 if-else 判断状态
 *
 * <pre>
 * 10 待支付 ──支付成功──> 20 待接单 ──商家接单──> 30 备餐中 ──出餐──> 40 配送中 ──> 50 已完成
 *    │                       │                      │
 *    └─顾客取消/超时─> 60 已取消 <─商家拒单/顾客取消──┘
 * </pre>
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(10, "待支付"),
    PENDING_ACCEPT(20, "待接单"),
    PREPARING(30, "备餐中"),
    DELIVERING(40, "配送中"),
    COMPLETED(50, "已完成"),
    CANCELED(60, "已取消"),
    REFUNDED(70, "已退款");

    private final Integer code;
    private final String desc;

    /**
     * 流转表：key = 起始状态，value = 允许到达的状态集合
     *
     * <p>
     * 只登记一期真实可达的流转，避免出现「表里允许但代码里永远不会走」的死规则。
     * REFUNDED(70) 作为状态先占位，对应的流转在二期「平台介入退款」时再加。
     * </p>
     */
    private static final Map<Integer, java.util.Set<Integer>> TRANSITIONS = Map.of(
            PENDING_PAYMENT.code, java.util.Set.of(PENDING_ACCEPT.code, CANCELED.code),
            PENDING_ACCEPT.code, java.util.Set.of(PREPARING.code, CANCELED.code),
            PREPARING.code, java.util.Set.of(DELIVERING.code, CANCELED.code),
            DELIVERING.code, java.util.Set.of(COMPLETED.code));

    public static OrderStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /** 判断一次状态流转是否被允许 */
    public static boolean canTransfer(Integer from, Integer to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, java.util.Set.of()).contains(to);
    }

    /** 一次流转的允许目标，用于异常提示 */
    public static String allowedTargets(Integer from) {
        java.util.Set<Integer> targets = TRANSITIONS.getOrDefault(from, java.util.Set.of());
        if (targets.isEmpty()) {
            return "无（终态）";
        }
        return targets.stream()
                .map(code -> {
                    OrderStatusEnum e = of(code);
                    return e == null ? String.valueOf(code) : e.desc;
                })
                .collect(Collectors.joining("、"));
    }

    /** 所有状态描述，便于前端渲染 */
    public static Map<Integer, String> allDesc() {
        return Arrays.stream(values()).collect(Collectors.toMap(OrderStatusEnum::getCode, OrderStatusEnum::getDesc));
    }

    /** 是否为终态（不可再流转） */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELED || this == REFUNDED;
    }
}