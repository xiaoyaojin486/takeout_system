package com.takeout.user.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.user.dto.ResetPwdDTO;
import com.takeout.user.dto.UpdateStatusDTO;
import com.takeout.user.dto.UserQuery;
import com.takeout.user.service.UserService;
import com.takeout.user.vo.UserVO;
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
 * 管理后台 - 账号管理接口
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@Tag(name = "管理后台-账号管理")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "账号分页", description = "可按关键词/身份/状态筛选")
    public Result<PageResult<UserVO>> page(@Validated UserQuery query) {
        return Result.ok(userService.page(query));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用账号", description = "管理员账号不允许停用")
    public Result<Void> updateStatus(@PathVariable Long id, @Validated @RequestBody UpdateStatusDTO dto) {
        userService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置账号密码")
    public Result<Void> resetPassword(@PathVariable Long id, @Validated @RequestBody ResetPwdDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return Result.ok();
    }
}