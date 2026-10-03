package com.takeout.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料 DTO
 *
 * <p>只允许改昵称/头像/手机号：账号名与用户类型不可自助修改，
 * 避免顾客把自己改成商家。</p>
 */
@Data
@Schema(description = "修改个人资料入参")
public class UpdateProfileDTO {

    @Size(max = 20, message = "昵称长度不能超过 20")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}