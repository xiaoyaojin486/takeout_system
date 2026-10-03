package com.takeout.common.interceptor;

import com.takeout.common.context.LoginUser;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.UserTypeEnum;
import com.takeout.common.exception.ForbiddenException;
import com.takeout.common.filter.AuthenticationFilter;
import com.takeout.common.properties.SecurityProperties;
import com.takeout.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 端级授权拦截器（第二道闸）：按路径前缀卡住「哪一端能访问」
 *
 * <p>
 * 把整段前缀的权限收口到一处，优于在每个 Controller 上散落注解 ——
 * 新增接口时只要放进对应前缀目录，权限自动生效，不会漏配。
 * </p>
 *
 * <table>
 * <tr>
 * <th>路径前缀</th>
 * <th>允许的用户类型</th>
 * </tr>
 * <tr>
 * <td>/customer/**</td>
 * <td>1 顾客</td>
 * </tr>
 * <tr>
 * <td>/merchant/**</td>
 * <td>2 商家</td>
 * </tr>
 * <tr>
 * <td>/admin/**</td>
 * <td>3 管理员</td>
 * </tr>
 * </table>
 *
 * <p>
 * <b>必须放行白名单</b>：顾客注册接口的路径是 /customer/user/register，
 * 虽然落在 /customer 前缀下，但它是免登录接口。若不放行，
 * 注册请求会在这里被要求「先登录才能注册」而死锁。
 * </p>
 *
 * <p>
 * 注意：这里只能卡到「端」，卡不到「哪条数据」。数据归属校验（商家只能操作自己的店铺/菜品/订单）
 * 必须在各模块 Service 层用 UserContextHolder 的 userId 再校验一次。
 * </p>
 */
public class UserTypeInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final String CUSTOMER_PATTERN = "/customer/**";
    private static final String MERCHANT_PATTERN = "/merchant/**";
    private static final String ADMIN_PATTERN = "/admin/**";

    private final SecurityProperties securityProperties;

    public UserTypeInterceptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = AuthenticationFilter.stripContextPath(request);

        // 免登录接口直接放行（认证过滤器已经放行，这里不能重复拦截）
        if (isWhitelist(uri)) {
            return true;
        }

        Integer requiredType = null;
        if (MATCHER.match(CUSTOMER_PATTERN, uri)) {
            requiredType = UserTypeEnum.CUSTOMER.getCode();
        } else if (MATCHER.match(MERCHANT_PATTERN, uri)) {
            requiredType = UserTypeEnum.MERCHANT.getCode();
        } else if (MATCHER.match(ADMIN_PATTERN, uri)) {
            requiredType = UserTypeEnum.ADMIN.getCode();
        }

        if (requiredType == null) {
            // 非三端前缀（如 /auth/me、/file/upload）：只要通过认证过滤器即可
            return true;
        }

        LoginUser loginUser = UserContextHolder.requireLogin();
        if (!requiredType.equals(loginUser.getUserType())) {
            UserTypeEnum actual = UserTypeEnum.of(loginUser.getUserType());
            UserTypeEnum required = UserTypeEnum.of(requiredType);
            throw new ForbiddenException(ResultCode.FORBIDDEN,
                    "该接口仅限" + (required == null ? requiredType : required.getDesc())
                            + "访问，当前身份：" + (actual == null ? loginUser.getUserType() : actual.getDesc()));
        }
        return true;
    }

    private boolean isWhitelist(String uri) {
        for (String pattern : securityProperties.getWhitelist()) {
            if (MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}