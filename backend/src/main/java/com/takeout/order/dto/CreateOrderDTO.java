package com.takeout.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 下单 DTO
 *
 * <p><b>只有 dishId 和 quantity，没有价格</b>——这是刻意的。
 * 金额一律由后端读菜品表重算，前端传什么价格都不认，从源头杜绝改价攻击。</p>
 */
@Data
@Schema(description = "下单入参")
public class CreateOrderDTO {

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    @NotEmpty(message = "下单菜品不能为空")
    @Valid
    @Schema(description = "下单明细；同一订单的菜品必须属于同一家店铺")
    private List<OrderItemDTO> items;

    @Size(max = 200, message = "备注不能超过 200 字")
    @Schema(description = "顾客备注")
    private String remark;
}