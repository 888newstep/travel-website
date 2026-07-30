package travel.common.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.Result;

import java.util.List;
import java.util.Map;

/**
 * Sentinel 限流/熔断降级处理
 */
@Slf4j
public final class SentinelFallbackHandler {

    private SentinelFallbackHandler() {}

    /**
     * 景点查询限流降级
     */
    public static Result<Attraction> getAttractionByIdFallback(Integer id, BlockException e) {
        log.warn("景点查询被限流: id={}", id);
        return Result.error(429, "请求过于频繁，请稍后再试");
    }

    public static Result<List<Attraction>> getAttractionsByCityFallback(Integer cityId, BlockException e) {
        log.warn("城市景点查询被限流: cityId={}", cityId);
        return Result.error(429, "请求过于频繁，请稍后再试");
    }

    public static Result<List<Attraction>> searchAttractionsFallback(String keyword, BlockException e) {
        log.warn("景点搜索被限流: keyword={}", keyword);
        return Result.error(429, "搜索请求过多，请稍后再试");
    }

    /**
     * 路线规划限流降级
     */
    public static Result<Map<String, Object>> planOptimalRouteFallback(
            List<Integer> attractionIds, int maxDays, Object budget, String preference, BlockException e) {
        log.warn("路线规划被限流: attractionCount={}", attractionIds != null ? attractionIds.size() : 0);
        return Result.error(429, "路线规划请求过多，请稍后再试");
    }

    /**
     * 收藏限流降级
     */
    public static Result<Map<String, Object>> collectRouteFallback(Long routeId, Long userId, BlockException e) {
        log.warn("收藏操作被限流: routeId={}, userId={}", routeId, userId);
        return Result.error(429, "操作过于频繁，请稍后再试");
    }

    /**
     * 通用降级
     */
    public static Result<Object> commonFallback(BlockException e) {
        log.warn("接口被限流/熔断: {}", e.getMessage());
        return Result.error(429, "服务繁忙，请稍后再试");
    }
}