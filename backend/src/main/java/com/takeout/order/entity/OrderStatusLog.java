package com.takeout.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态流转日志实体（只新增，不修改）
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Integer fromStatus;

    private Integer toStatus;

    /** 1 顾客 / 2 商家 / 3 系统 / 4 管理员 */
    private Integer operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;
}