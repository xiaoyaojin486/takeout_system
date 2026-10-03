package com.takeout.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.takeout.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：create_time / update_time / create_by / update_by / del_flag
 *
 * <p>好处是业务代码里看不到一行「setCreateTime(now())」，Entity 也就不必到处 new Date()。
 * createBy 取当前登录用户；系统触发的写入（定时任务、演示数据）记为 0 = 系统。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /** 0 表示系统操作（定时任务、初始化） */
    private static final Long SYSTEM_USER = 0L;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operator = currentOperator();

        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, operator);
        strictInsertFill(metaObject, "updateBy", Long.class, operator);
        strictInsertFill(metaObject, "delFlag", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // update_time/update_by 必须覆盖旧值，因此用 setFieldValByName 而不是 strictUpdateFill
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        setFieldValByName("updateBy", currentOperator(), metaObject);
    }

    private Long currentOperator() {
        Long userId = UserContextHolder.getUserIdOrNull();
        return userId == null ? SYSTEM_USER : userId;
    }
}