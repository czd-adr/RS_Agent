package com.xmut.shop.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 活动图层存储：记录每个会话当前在地图上显示的活动图层
 * Redis Key 格式：rs:session:{memoryId}:active_layers（Set 结构，成员为图层名）
 * 数据行过期时间：2 小时
 */
@Repository
public class ActiveLayerStore {

    private static final String KEY_PREFIX = "rs:session:";
    private static final String KEY_SUFFIX = ":active_layers";

    // 数据行过期时间：2 小时
    private static final Duration TTL = Duration.ofHours(2);

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String key(String memoryId) {
        return KEY_PREFIX + memoryId + KEY_SUFFIX;
    }

    /**
     * 新增活动图层（Set 去重），每次写入刷新 2 小时过期时间
     */
    public void add(String memoryId, String layerName) {
        String k = key(memoryId);
        redisTemplate.opsForSet().add(k, layerName);
        redisTemplate.expire(k, TTL);
    }

    /**
     * 删除活动图层
     */
    public void remove(String memoryId, String layerName) {
        String k = key(memoryId);
        redisTemplate.opsForSet().remove(k, layerName);
        redisTemplate.expire(k, TTL);
    }

    /**
     * 查询活动图层列表（按名称排序），读取时刷新过期时间保持会话活跃
     */
    public List<String> list(String memoryId) {
        String k = key(memoryId);
        Set<String> members = redisTemplate.opsForSet().members(k);
        List<String> result = new ArrayList<>(members == null ? Collections.emptySet() : members);
        Collections.sort(result);
        redisTemplate.expire(k, TTL);
        return result;
    }

    /**
     * 清空会话的全部活动图层（配合删除会话使用）
     */
    public void clear(String memoryId) {
        redisTemplate.delete(key(memoryId));
    }
}
