package com.takeout.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 超级管理员账号 id（演示数据里的 admin） */
    public static final Long ADMIN_USER_ID = 1L;

    /** 未删除 */
    public static final Integer NOT_DELETED = 0;

    /** 已删除 */
    public static final Integer DELETED = 1;

    /** 启用/正常 */
    public static final Integer STATUS_ENABLED = 1;

    /** 停用/禁用 */
    public static final Integer STATUS_DISABLED = 0;

    /** 页面请求头：标记当前端（仅用于日志排查，权限判定一律以后端 userType 为准） */
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
}