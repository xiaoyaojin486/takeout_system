package com.takeout.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置他人密码 DTO
 */
@Data
@Schema(description = "重置密码入参")
public class ResetPwdDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需在 6~20 之间")
    @Schema(description = "新密码")
    private String newPassword;
}