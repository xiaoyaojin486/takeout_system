package com.takeout.category.controller;

import com.takeout.category.dto.CategoryDTO;
import com.takeout.category.service.CategoryService;
import com.takeout.category.vo.CategoryVO;
import com.takeout.common.result.Result;
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

import java.util.List;

/**
 * 商家端 - 分类管理接口
 */
@RestController
@RequestMapping("/merchant/category")
@RequiredArgsConstructor
@Tag(name = "商家端-分类管理")
public class MerchantCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "我的分类列表", description = "按 sort 升序")
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listMine());
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public Result<Void> create(@Validated @RequestBody CategoryDTO dto) {
        categoryService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑分类")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}