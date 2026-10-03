package com.takeout.merchant.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.merchant.dto.AuditDTO;
import com.takeout.merchant.dto.BusinessStatusDTO;
import com.takeout.merchant.dto.MerchantQuery;
import com.takeout.merchant.service.MerchantService;
import com.takeout.merchant.vo.MerchantVO;
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
 * 管理后台 - 店铺管理接口
 */
@RestController
@RequestMapping("/admin/merchant")
@RequiredArgsConstructor
@Tag(name = "管理后台-店铺管理")
public class AdminMerchantController {

    private final MerchantService merchantService;

    @GetMapping
    @Operation(summary = "店铺分页", description = "可按关键词/审核状态/营业状态筛选")
    public Result<PageResult<MerchantVO>> page(@Validated MerchantQuery query) {
        return Result.ok(merchantService.adminPage(query));
    }

    @PutMapping("/{id}/audit")
    @Operation(summary = "审核店铺", description = "1 通过 / 2 驳回；通过时营业状态归零，由商家自行开启")
    public Result<Void> audit(@PathVariable Long id, @Validated @RequestBody AuditDTO dto) {
        merchantService.audit(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/business-status")
    @Operation(summary = "强制修改营业状态", description = "管理员强制打烊/恢复")
    public Result<Void> updateBusinessStatus(@PathVariable Long id, @Validated @RequestBody BusinessStatusDTO dto) {
        merchantService.updateBusinessStatusByAdmin(id, dto.getBusinessStatus());
        return Result.ok();
    }
}