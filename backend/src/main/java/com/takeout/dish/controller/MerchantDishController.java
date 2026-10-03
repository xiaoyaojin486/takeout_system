package com.takeout.dish.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.dish.dto.DishDTO;
import com.takeout.dish.dto.DishQuery;
import com.takeout.dish.dto.DishStatusDTO;
import com.takeout.dish.service.DishService;
import com.takeout.dish.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端 - 菜品管理接口
 */
@RestController
@RequestMapping("/merchant/dish")
@RequiredArgsConstructor
@Tag(name = "商家端-菜品管理")
public class MerchantDishController {

    private final DishService dishService;

    @GetMapping
    @Operation(summary = "菜品分页", description = "仅返回自己的菜品，可按关键词/分类/状态筛选")
    public Result<PageResult<DishVO>> page(@Validated DishQuery query) {
        return Result.ok(dishService.merchantPage(query));
    }

    @PostMapping
    @Operation(summary = "新增菜品", description = "分类必须属于自己")
    public Result<Void> create(@Validated @RequestBody DishDTO dto) {
        dishService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑菜品")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody DishDTO dto) {
        dishService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜品")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "菜品上下架")
    public Result<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody DishStatusDTO dto) {
        dishService.updateStatus(id, dto);
        return Result.ok();
    }
}