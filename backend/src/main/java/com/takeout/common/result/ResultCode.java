package com.takeout.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码
 *
 * <p>
 * 码段划分：4xx/5xx 走 HTTP 语义，其余按业务域分段，便于前端按段做统一处理。
 * </p>
 * <ul>
 * <li>40101 / 40102 —— 双 token 专用：前端据此决定「自动续期」还是「跳登录」</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录状态已失效"),
    /** 访问令牌过期：前端拦截器应静默刷新后重放请求 */
    ACCESS_TOKEN_EXPIRED(40101, "访问令牌已过期，请刷新令牌"),
    /** 刷新令牌失效：前端必须清空登录态并跳转登录页 */
    REFRESH_TOKEN_INVALID(40102, "刷新令牌已失效，请重新登录"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    ERROR(500, "系统异常，请稍后再试"),

    /** 账号 10xxx */
    USERNAME_PASSWORD_ERROR(10001, "用户名或密码错误"),
    ACCOUNT_DISABLED(10002, "账号已停用，请联系管理员"),
    USERNAME_EXISTS(10003, "登录账号已存在"),
    PHONE_EXISTS(10004, "手机号已被注册"),
    OLD_PASSWORD_ERROR(10005, "原密码不正确"),
    USER_NOT_FOUND(10006, "用户不存在"),
    CLIENT_TYPE_NOT_ALLOWED(10007, "该账号不允许登录当前端"),

    /** 店铺 11xxx */
    MERCHANT_NOT_FOUND(11001, "店铺不存在"),
    MERCHANT_NOT_AUDITED(11002, "店铺未通过审核，暂不可用"),
    MERCHANT_CLOSED(11003, "店铺休息中，暂不接单"),
    MERCHANT_EXISTS(11004, "该账号已开通店铺"),
    MERCHANT_NOT_OWNER(11005, "无权操作该店铺"),

    /** 菜品 12xxx */
    CATEGORY_NOT_FOUND(12001, "分类不存在"),
    CATEGORY_HAS_DISH(12002, "分类下存在菜品，不允许删除"),
    DISH_NOT_FOUND(12003, "菜品不存在"),
    DISH_OFF_SHELF(12004, "菜品已下架"),
    STOCK_NOT_ENOUGH(12005, "菜品库存不足"),

    /** 购物车 / 地址 13xxx */
    CART_EMPTY(13001, "购物车为空"),
    CART_ITEM_NOT_FOUND(13002, "购物车中没有该菜品"),
    ADDRESS_NOT_FOUND(13003, "收货地址不存在"),
    ADDRESS_NOT_OWNER(13004, "无权操作该地址"),
    CART_MULTI_MERCHANT(13005, "一个订单只能包含同一家店铺的菜品"),

    /** 订单 / 支付 14xxx */
    ORDER_NOT_FOUND(14001, "订单不存在"),
    ORDER_STATUS_ERROR(14002, "订单当前状态不允许该操作"),
    BELOW_MIN_AMOUNT(14003, "未达到店铺起送价"),
    ORDER_NOT_OWNER(14004, "无权操作该订单"),
    PAYMENT_NOT_FOUND(14005, "支付单不存在"),
    PAYMENT_STATUS_ERROR(14006, "支付单状态异常"),

    /** 通用防护 15xxx */
    REPEAT_SUBMIT(15001, "请勿重复提交，请稍后再试"),
    IDEMPOTENT_PROCESSING(15002, "操作正在处理中，请勿重复操作"),
    RATE_LIMITED(15003, "请求过于频繁，请稍后再试"),
    LOGIN_TOO_MANY_FAILURES(15004, "密码错误次数过多，请 10 分钟后再试"),

    /** 文件 16xxx */
    FILE_EMPTY(16001, "上传文件为空"),
    FILE_TYPE_ERROR(16002, "仅支持 jpg / jpeg / png / gif / webp 图片"),
    FILE_TOO_LARGE(16003, "文件大小不能超过 5MB"),
    FILE_UPLOAD_ERROR(16004, "文件上传失败");

    private final Integer code;
    private final String message;
}