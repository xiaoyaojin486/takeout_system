package com.takeout.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 缓存实现：生产推荐
 *
 * <p>启用方式：.env 中设置 {@code CACHE_TYPE=redis} 并配置 REDIS_HOST/REDIS_PORT。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "takeout.cache", name = "type", havingValue = "redis")
public class RedisCacheClient implements CacheClient {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void set(String key, String value, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public long increment(String key, long ttlSeconds) {
        Long value = redisTemplate.opsForValue().increment(key);
        long count = value == null ? 0L : value;
        // 只在第一次自增时设置 TTL，保证是「固定窗口」而不是「滑动窗口」
        if (count == 1L && ttlSeconds > 0) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        return count;
    }

    @Override
    public void expire(String key, long ttlSeconds) {
        if (ttlSeconds > 0 && Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
    }
}