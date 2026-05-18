package travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import travel.utils.CacheUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitorService {

    private final CacheUtil cacheUtil;
    private final Map<String, Long> metrics = new ConcurrentHashMap<>();

    /**
     * 获取缓存命中率
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        Long hits = cacheUtil.getCount(
                CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits")
        );
        Long misses = cacheUtil.getCount(
                CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses")
        );

        stats.put("hits", hits);
        stats.put("misses", misses);
        stats.put("hitRate", hits + misses > 0 ?
                (double) hits / (hits + misses) * 100 : 0);

        return stats;
    }

    /**
     * 记录API调用
     */
    public void recordApiCall(String apiName, long duration) {
        metrics.compute(apiName, (k, v) -> v == null ? duration : v + duration);

        cacheUtil.increment(
                CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "api_calls", apiName),
                1
        );
    }

    /**
     * 记录任务执行
     */
    public void recordTaskExecution(String taskType, long duration) {
        metrics.compute("TASK_" + taskType, (k, v) -> v == null ? duration : v + duration);

        cacheUtil.increment(
                CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "task_executions", taskType),
                1
        );

        log.debug("记录任务执行: taskType={}, duration={}ms", taskType, duration);
    }

    /**
     * 定时输出性能指标
     */
    @Scheduled(fixedRate = 60000)
    public void reportMetrics() {
        log.info("=== 性能指标报告 ===");
        metrics.forEach((api, duration) -> {
            log.info("API/Task: {}, 总耗时: {}ms", api, duration);
        });
        metrics.clear();
    }

}
