package com.takeout.address.controller;

import com.takeout.address.dto.AddressDTO;
import com.takeout.address.service.AddressService;
import com.takeout.address.vo.AddressVO;
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
 * 顾客端 - 收货地址接口
 */
@RestController
@RequestMapping("/customer/address")
@RequiredArgsConstructor
@Tag(name = "顾客端-收货地址")
public class CustomerAddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "地址列表", description = "默认地址排最前")
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list());
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public Result<Void> create(@Validated @RequestBody AddressDTO dto) {
        addressService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改地址")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody AddressDTO dto) {
        addressService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }
}