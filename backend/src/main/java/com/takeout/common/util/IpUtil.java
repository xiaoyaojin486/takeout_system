package com.takeout.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP 工具：限流按 IP 计数时需要拿到可信的客户端 IP
 */
public final class IpUtil {

    private IpUtil() {
    }

    private static final String UNKNOWN = "unknown";

    /**
     * 取客户端 IP：按代理头优先级依次尝试，全部取不到时回落到 remoteAddr
     *
     * <p>注意：这些头可被客户端伪造，仅用于限流这类「提高攻击成本」的场景，
     * 不能作为安全边界（真正的安全边界是 JWT 签名）。</p>
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            // XFF 可能是「客户端IP, 代理1, 代理2」，取第一个
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return ip.trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (isValid(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }
}