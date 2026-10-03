package com.takeout.merchant.dto;

import com.takeout.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺分页查询条件（顾客端与管理后台共用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "店铺查询条件")
public class MerchantQuery extends PageQuery {

    @Schema(description = "关键词：店铺名模糊匹配")
    private String keyword;

    @Schema(description = "0 待审核 / 1 已通过 / 2 已驳回")
    private Integer auditStatus;

    @Schema(description = "1 营业中 / 0 休息中")
    private Integer businessStatus;
}