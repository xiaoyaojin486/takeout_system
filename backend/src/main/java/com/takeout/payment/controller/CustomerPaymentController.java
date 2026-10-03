package com.takeout.payment.controller;

import com.takeout.common.annotation.RepeatSubmit;
import com.takeout.common.result.Result;
import com.takeout.payment.dto.PayDTO;
import com.takeout.payment.service.PaymentService;
import com.takeout.payment.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端 - 支付接口
 */
@RestController
@RequestMapping("/customer/payment")
@RequiredArgsConstructor
@Tag(name = "顾客端-支付")
public class CustomerPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderNo}/pay")
    @RepeatSubmit(interval = 3)
    @Operation(summary = "发起支付（模拟）",
            description = "一期为模拟支付：直接置为已支付并把订单推进到待接单。重复调用幂等返回。")
    public Result<PaymentVO> pay(@PathVariable String orderNo,
                                 @Validated @RequestBody(required = false) PayDTO dto) {
        return Result.ok(paymentService.pay(orderNo, dto == null ? null : dto.getPayType()));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单的支付信息", description = "支付单不存在时返回 data 为 null")
    public Result<PaymentVO> getByOrderNo(@PathVariable String orderNo) {
        return Result.ok(paymentService.getByOrderNo(orderNo));
    }
}