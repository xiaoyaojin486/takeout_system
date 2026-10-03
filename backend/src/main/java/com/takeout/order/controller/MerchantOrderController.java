package com.takeout.order.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.order.dto.CancelOrderDTO;
import com.takeout.order.dto.OrderQuery;
import com.takeout.order.dto.RejectDTO;
import com.takeout.order.service.OrderService;
import com.takeout.order.vo.OrderDetailVO;
import com.takeout.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端 - 订单接口
 *
 * <p>所有操作都会经过「订单归属校验」：只能操作自己店铺的订单。</p>
 */
@RestController
@RequestMapping("/merchant/order")
@RequiredArgsConstructor
@Tag(name = "商家端-订单")
public class MerchantOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "订单分页", description = "可按状态筛选，默认按下单时间倒序")
    public Result<PageResult<OrderVO>> page(@Validated OrderQuery query) {
        return Result.ok(orderService.merchantOrders(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.merchantOrderDetail(id));
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "接单", description = "待接单 → 备餐中")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.acceptByMerchant(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒单", description = "待接单 → 已取消，归还库存")
    public Result<Void> reject(@PathVariable Long id, @Validated @RequestBody RejectDTO dto) {
        orderService.rejectByMerchant(id, dto.getReason());
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "待接单/备餐中可用，归还库存")
    public Result<Void> cancel(@PathVariable Long id,
                               @Validated @RequestBody(required = false) CancelOrderDTO dto) {
        orderService.cancelByMerchant(id, dto == null ? null : dto.getReason());
        return Result.ok();
    }

    @PutMapping("/{id}/deliver")
    @Operation(summary = "出餐", description = "备餐中 → 配送中")
    public Result<Void> deliver(@PathVariable Long id) {
        orderService.deliverByMerchant(id);
        return Result.ok();
    }

    @PutMapping("/{id}/finish")
    @Operation(summary = "送达", description = "配送中 → 已完成，累加销量")
    public Result<Void> finish(@PathVariable Long id) {
        orderService.finishByMerchant(id);
        return Result.ok();
    }
}