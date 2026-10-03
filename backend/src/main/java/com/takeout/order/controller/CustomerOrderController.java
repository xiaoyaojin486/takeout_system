package com.takeout.order.controller;

import com.takeout.common.annotation.Idempotent;
import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.order.dto.CancelOrderDTO;
import com.takeout.order.dto.CreateOrderDTO;
import com.takeout.order.dto.OrderQuery;
import com.takeout.order.service.OrderService;
import com.takeout.order.vo.OrderCreateVO;
import com.takeout.order.vo.OrderDetailVO;
import com.takeout.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端 - 订单接口
 */
@RestController
@RequestMapping("/customer/order")
@RequiredArgsConstructor
@Tag(name = "顾客端-订单")
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    @Idempotent(ttl = 300)
    @Operation(summary = "下单",
            description = "金额由后端重算；可带 X-Idempotent-Token 头做幂等，重复提交会被拦截")
    public Result<OrderCreateVO> create(@Validated @RequestBody CreateOrderDTO dto) {
        return Result.ok(orderService.createOrder(dto));
    }

    @GetMapping
    @Operation(summary = "我的订单分页")
    public Result<PageResult<OrderVO>> myOrders(@Validated OrderQuery query) {
        return Result.ok(orderService.myOrders(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的订单详情", description = "含状态流转时间轴")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.myOrderDetail(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "仅待支付/待接单可取消，会归还库存")
    public Result<Void> cancel(@PathVariable Long id,
                               @Validated @RequestBody(required = false) CancelOrderDTO dto) {
        orderService.cancelByCustomer(id, dto == null ? null : dto.getReason());
        return Result.ok();
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认收货", description = "配送中 → 已完成，累加销量")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmByCustomer(id);
        return Result.ok();
    }
}