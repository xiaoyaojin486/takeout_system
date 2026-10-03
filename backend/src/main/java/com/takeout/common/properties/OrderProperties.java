package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 订单域配置
 */
@Data
@ConfigurationProperties(prefix = "takeout.order")
public class OrderProperties {

    /** 未支付订单超时时间（分钟），到期由定时任务自动取消并归还库存 */
    private Integer payTimeoutMinutes = 15;
}