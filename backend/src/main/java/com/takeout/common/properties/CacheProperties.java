package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存实现配置：local（内存）/ redis
 */
@Data
@ConfigurationProperties(prefix = "takeout.cache")
public class CacheProperties {

    /** local / redis */
    private String type = "local";
}