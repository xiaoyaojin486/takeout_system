package com.takeout.category.controller;

import com.takeout.category.service.CategoryService;
import com.takeout.category.vo.CategoryVO;
import com.takeout.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端 - 分类接口
 */
@RestController
@RequestMapping("/customer/category")
@RequiredArgsConstructor
@Tag(name = "顾客端-分类")
public class CustomerCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "店铺分类列表", description = "仅返回启用中的分类")
    public Result<List<CategoryVO>> list(@RequestParam Long merchantId) {
        return Result.ok(categoryService.customerListByMerchant(merchantId));
    }
}