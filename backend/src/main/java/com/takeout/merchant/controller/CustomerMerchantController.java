package com.takeout.merchant.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.merchant.dto.MerchantQuery;
import com.takeout.merchant.service.MerchantService;
import com.takeout.merchant.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端 - 店铺接口
 */
@RestController
@RequestMapping("/customer/merchant")
@RequiredArgsConstructor
@Tag(name = "顾客端-店铺")
public class CustomerMerchantController {

    private final MerchantService merchantService;

    @GetMapping("/list")
    @Operation(summary = "店铺列表", description = "分页 + 关键词，仅返回审核通过的店铺，按销量倒序")
    public Result<PageResult<MerchantVO>> list(@Validated MerchantQuery query) {
        return Result.ok(merchantService.customerPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "店铺详情")
    public Result<MerchantVO> detail(@PathVariable Long id) {
        return Result.ok(merchantService.customerDetail(id));
    }
}