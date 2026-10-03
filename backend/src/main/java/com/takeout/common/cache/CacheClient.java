package com.takeout.common.cache;

/**
 * 缓存抽象：双 token 的 refresh 存储 / 黑名单 / 限流 / 防重全部走它
 *
 * <p>业务代码只依赖本接口，实现可随时在 local 与 redis 之间切换，
 * 切换方式是改 .env 的 CACHE_TYPE，不动任何业务代码。</p>
 */
public interface CacheClient {

    /**
     * 写入并设置过期时间
     *
     * @param ttlSeconds 存活秒数，<= 0 表示永不过期（仅 local 实现支持）
     */
    void set(String key, String value, long ttlSeconds);

    /** 读取，过期视为不存在 */
    String get(String key);

    /** 删除（不存在时静默返回） */
    void delete(String key);

    /** 是否存在且未过期 */
    boolean hasKey(String key);

    /**
     * 原子自增：不存在则从 1 开始并设置 TTL
     *
     * @return 自增后的值
     */
    long increment(String key, long ttlSeconds);

    /** 重设过期时间，key 不存在时忽略 */
    void expire(String key, long ttlSeconds);
}