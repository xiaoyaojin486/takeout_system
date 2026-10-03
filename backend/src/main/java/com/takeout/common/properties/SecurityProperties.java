package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置（认证白名单）
 */
@Data
@ConfigurationProperties(prefix = "takeout.security")
public class SecurityProperties {

    /** 无需登录即可访问的路径，Ant 风格 */
    private List<String> whitelist = new ArrayList<>();
}