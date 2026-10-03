package com.takeout.cart.controller;

import com.takeout.cart.dto.CartAddDTO;
import com.takeout.cart.dto.CartUpdateDTO;
import com.takeout.cart.service.CartService;
import com.takeout.cart.vo.CartShopGroupVO;
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
 * 顾客端 - 购物车接口
 */
@RestController
@RequestMapping("/customer/cart")
@RequiredArgsConstructor
@Tag(name = "顾客端-购物车")
public class CustomerCartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "购物车列表", description = "按店铺分组，金额实时按菜品表计算")
    public Result<List<CartShopGroupVO>> list() {
        return Result.ok(cartService.list());
    }

    @GetMapping("/count")
    @Operation(summary = "购物车件数", description = "H5 底部角标用")
    public Result<Integer> count() {
        return Result.ok(cartService.count());
    }

    @PostMapping
    @Operation(summary = "加入购物车", description = "同菜品重复加购会累加数量")
    public Result<Void> add(@Validated @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改数量", description = "数量传 0 等价于移除")
    public Result<Void> updateQuantity(@PathVariable Long id, @Validated @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(id, dto.getQuantity());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除菜品")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "清空购物车")
    public Result<Void> clear() {
        cartService.clear();
        return Result.ok();
    }
}