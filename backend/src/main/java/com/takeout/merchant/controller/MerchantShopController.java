package com.takeout.merchant.controller;

import com.takeout.common.result.Result;
import com.takeout.merchant.dto.BusinessStatusDTO;
import com.takeout.merchant.dto.ShopDTO;
import com.takeout.merchant.service.MerchantService;
import com.takeout.merchant.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端 - 店铺设置接口
 *
 * <p>Controller 只做「接参 + 校验 + 转发」，不写业务逻辑，不直接调用 Mapper。</p>
 */
@RestController
@RequestMapping("/merchant/shop")
@RequiredArgsConstructor
@Tag(name = "商家端-店铺设置")
public class MerchantShopController {

    private final MerchantService merchantService;

    @GetMapping
    @Operation(summary = "获取我的店铺")
    public Result<MerchantVO> getMyShop() {
        return Result.ok(merchantService.getMyShop());
    }

    @PostMapping("/apply")
    @Operation(summary = "商家入驻申请", description = "同一账号只能开通一个店铺，提交后进入待审核")
    public Result<Void> apply(@Validated @RequestBody ShopDTO dto) {
        merchantService.applyShop(dto);
        return Result.ok();
    }

    @PutMapping
    @Operation(summary = "编辑我的店铺")
    public Result<Void> update(@Validated @RequestBody ShopDTO dto) {
        merchantService.updateMyShop(dto);
        return Result.ok();
    }

    @PutMapping("/business-status")
    @Operation(summary = "切换营业状态", description = "仅审核通过后可操作")
    public Result<Void> updateBusinessStatus(@Validated @RequestBody BusinessStatusDTO dto) {
        merchantService.updateBusinessStatus(dto.getBusinessStatus());
        return Result.ok();
    }
}