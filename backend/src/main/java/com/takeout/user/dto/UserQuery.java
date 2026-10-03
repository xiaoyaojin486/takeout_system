package com.takeout.user.dto;

import com.takeout.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号分页查询条件（管理后台）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "账号查询条件")
public class UserQuery extends PageQuery {

    @Schema(description = "关键词：账号 / 昵称 / 手机号 模糊匹配")
    private String keyword;

    @Schema(description = "1 顾客 / 2 商家 / 3 管理员")
    private Integer userType;

    @Schema(description = "1 正常 / 0 停用")
    private Integer status;
}