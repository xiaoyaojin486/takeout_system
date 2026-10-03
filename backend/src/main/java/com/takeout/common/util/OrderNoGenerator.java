package com.takeout.common.util;

import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;

/**
 * 业务单号生成器：日期 + 当日自增序号 + 随机后缀
 *
 * <p>为什么加随机后缀：local 缓存实现下进程重启会丢失序号，
 * 只靠序号可能与重启前的历史单号重复（订单号有唯一索引，重复会直接报错）。
 * 日期+序号保证可读性与趋势感，随机后缀兜底唯一性。</p>
 */
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 序号缓存存活 2 天，跨天自然重置 */
    private static final long SEQ_TTL_SECONDS = 2 * 24 * 3600L;

    private final CacheClient cacheClient;

    /** 订单号：TO + yyyyMMdd + 6 位序号 + 4 位随机 => TO20260924000001A3K9 */
    public String nextOrderNo() {
        return generate("order", "TO");
    }

    /** 支付单号：PAY + yyyyMMdd + 6 位序号 + 4 位随机 */
    public String nextPayNo() {
        return generate("pay", "PAY");
    }

    /** 模拟支付流水号（真实支付场景由第三方返回） */
    public String mockTradeNo() {
        return "MOCK" + System.currentTimeMillis() + randomSuffix(6);
    }

    private String generate(String biz, String prefix) {
        String date = LocalDate.now().format(DATE_FMT);
        long seq = cacheClient.increment(CacheKeys.SEQ + biz + ":" + date, SEQ_TTL_SECONDS);
        return prefix + date + String.format("%06d", seq) + randomSuffix(4);
    }

    private String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}