package com.takeout.common.aspect;

import com.takeout.common.result.ResultCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 切面工具：从方法参数生成稳定的指纹（用于防重/幂等）
 */
public final class AspectUtils {

    private AspectUtils() {
    }

    /** 参数指纹：所有可序列化参数拼接后取 MD5，长度固定且不泄露原始参数 */
    public static String fingerprint(String prefix, String[] parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String part : parts) {
            sb.append('|').append(part == null ? "" : part);
        }
        return md5(sb.toString());
    }

    public static String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 是 JDK 标配算法，不可能走到这里
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    public static ResultCode repeatSubmitCode() {
        return ResultCode.REPEAT_SUBMIT;
    }
}