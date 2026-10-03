package com.takeout.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存实现（开发默认）：进程内 ConcurrentHashMap + 惰性过期
 *
 * <p>适用场景：本地开发、单机演示，无需安装 Redis 即可跑通双 token 全链路。</p>
 * <p><b>局限</b>：多实例部署时不共享（登出黑名单在 A 实例生效、B 实例不认），
 * 生产多副本必须切 CACHE_TYPE=redis。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "takeout.cache", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalCacheClient implements CacheClient {

    private record Entry(String value, long expireAt) {
        /** expireAt = 0 表示永不过期 */
        boolean expired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, long ttlSeconds) {
        store.put(key, new Entry(value, ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            // 惰性删除：读到过期项才清理，避免每个 key 都起定时器
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean hasKey(String key) {
        return get(key) != null;
    }

    @Override
    public long increment(String key, long ttlSeconds) {
        // compute 对同一 key 是原子的，天然满足「计数不丢」
        Entry entry = store.compute(key, (k, old) -> {
            if (old == null || old.expired()) {
                return new Entry("1", ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0);
            }
            return new Entry(String.valueOf(Long.parseLong(old.value()) + 1), old.expireAt());
        });
        return Long.parseLong(entry.value());
    }

    @Override
    public void expire(String key, long ttlSeconds) {
        store.computeIfPresent(key, (k, old) ->
                new Entry(old.value(), ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0));
    }

    /** 每分钟清理一次过期项，防止长期运行下内存堆积 */
    @Scheduled(fixedDelay = 60_000)
    public void cleanExpired() {
        int before = store.size();
        store.entrySet().removeIf(e -> e.getValue().expired());
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("本地缓存清理过期项 {} 个，剩余 {} 个", removed, store.size());
        }
    }
}