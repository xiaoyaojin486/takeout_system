package com.takeout.user.controller;

import com.takeout.common.result.Result;
import com.takeout.user.dto.ChangePasswordDTO;
import com.takeout.user.dto.RegisterDTO;
import com.takeout.user.dto.UpdateProfileDTO;
import com.takeout.user.service.UserService;
import com.takeout.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端 - 账号接口
 *
 * <p>Controller 只做「接参 + 校验 + 转发」，不写业务逻辑，不直接调用 Mapper。</p>
 */
@RestController
@RequestMapping("/customer/user")
@RequiredArgsConstructor
@Tag(name = "顾客端-账号")
public class CustomerUserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "顾客注册", description = "免登录接口，密码由后端 BCrypt 加密后落库")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料")
    public Result<UserVO> profile() {
        return Result.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "修改个人资料", description = "仅可改昵称/头像/手机号")
    public Result<Void> updateProfile(@Validated @RequestBody UpdateProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "需校验原密码，新密码 BCrypt 加密落库")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }
}