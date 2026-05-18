package travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import travel.utils.CacheUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控和统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMonitorService {

    private final CacheUtil cacheUtil;

    // 缓存命中率统计
    private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();

    /**
     * 记录缓存命中
     */
    public void recordHit(String cacheType) {
        cacheHits.computeIfAbsent(cacheType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss(String cacheType) {
        cacheMisses.computeIfAbsent(cacheType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 获取缓存统计信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalHits = cacheHits.values().stream().mapToLong(AtomicLong::get).sum();
        long totalMisses = cacheMisses.values().stream().mapToLong(AtomicLong::get).sum();
        long totalRequests = totalHits + totalMisses;

        stats.put("totalHits", totalHits);
        stats.put("totalMisses", totalMisses);
        stats.put("totalRequests", totalRequests);
        stats.put("hitRate", totalRequests > 0 ? (double) totalHits / totalRequests : 0.0);

        // 各类型缓存统计
        Map<String, Map<String, Long>> typeStats = new HashMap<>();
        for (String type : cacheHits.keySet()) {
            Map<String, Long> typeStat = new HashMap<>();
            typeStat.put("hits", cacheHits.get(type).get());
            typeStat.put("misses", cacheMisses.getOrDefault(type, new AtomicLong(0)).get());
            long typeTotal = typeStat.get("hits") + typeStat.get("misses");
            typeStat.put("hitRate", (long) (typeTotal > 0 ? (double) typeStat.get("hits") / typeTotal : 0.0));
            typeStats.put(type, typeStat);
        }
        stats.put("typeStats", typeStats);

        return stats;
    }

    /**
     * 定时清理统计数据（每小时重置一次）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void resetStats() {
        log.info("重置缓存统计数据");
        cacheHits.clear();
        cacheMisses.clear();
    }

    /**
     * 定时清理过期缓存（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredCache() {
        log.info("开始定时清理过期缓存...");

        try {
            // 清理AI相关缓存
            cacheUtil.deleteByPattern("ai:qa:*");
            cacheUtil.deleteByPattern("ai:recommend:*");

            // 清理高德地图缓存
            cacheUtil.deleteByPattern("amap:weather:*");
            cacheUtil.deleteByPattern("amap:traffic:*");

            log.info("定时清理过期缓存完成");
        } catch (Exception e) {
            log.error("定时清理缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取Redis连接状态
     */
    public boolean isRedisConnected() {
        try {
            return cacheUtil.exists("health:check");
        } catch (Exception e) {
            log.error("检查Redis连接失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
