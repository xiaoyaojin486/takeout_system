package com.takeout.dish.controller;

import com.takeout.common.result.Result;
import com.takeout.dish.service.DishService;
import com.takeout.dish.vo.DishGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端 - 菜品接口
 */
@RestController
@RequestMapping("/customer/dish")
@RequiredArgsConstructor
@Tag(name = "顾客端-菜品")
public class CustomerDishController {

    private final DishService dishService;

    @GetMapping("/menu")
    @Operation(summary = "店铺菜单", description = "仅返回上架菜品，按启用分类分组")
    public Result<List<DishGroupVO>> menu(@RequestParam Long merchantId) {
        return Result.ok(dishService.customerMenu(merchantId));
    }
}