package com.takeout.common.context;

import com.takeout.common.enums.UserTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户上下文对象
 *
 * <p>由 AuthenticationFilter 在鉴权通过后写入 ThreadLocal，
 * 业务层通过 {@link UserContextHolder} 读取，<b>一律以此对象为权限依据</b>，
 * 绝不使用前端传入的 userId/userType。</p>
 */
@Data
@Builder
public class LoginUser implements Serializable {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    /** 1 顾客 / 2 商家 / 3 管理员 */
    private Integer userType;

    public boolean isCustomer() {
        return UserTypeEnum.CUSTOMER.getCode().equals(userType);
    }

    public boolean isMerchant() {
        return UserTypeEnum.MERCHANT.getCode().equals(userType);
    }

    public boolean isAdmin() {
        return UserTypeEnum.ADMIN.getCode().equals(userType);
    }
}