package com.takeout.order.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 按日统计（跨模块契约 VO，供趋势图使用）
 */
@Data
public class DailyStatVO implements Serializable {

    private LocalDate statDate;

    private Integer orderCount;

    private BigDecimal amount;
}