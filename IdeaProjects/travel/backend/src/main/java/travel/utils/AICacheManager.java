package travel.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI服务缓存管理器
 * 统一管理AI相关服务的缓存策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AICacheManager {

    private final CacheUtil cacheUtil;

    // 缓存键前缀
    private static final String AI_QA_PREFIX = "ai:qa";
    private static final String AI_RECOMMEND_PREFIX = "ai:recommend";
    private static final String AI_ITINERARY_PREFIX = "ai:itinerary";
    private static final String AI_ATTRACTION_PREFIX = "ai:attraction";
    private static final String AI_TRANSLATE_PREFIX = "ai:translate";
    private static final String AI_CHAT_PREFIX = "ai:chat";
    private static final String AI_IMAGE_PREFIX = "ai:image";

    // 高德地图缓存键前缀
    private static final String AMAP_WEATHER_PREFIX = "amap:weather";
    private static final String AMAP_ROUTE_PREFIX = "amap:route";
    private static final String AMAP_PLACE_PREFIX = "amap:place";
    private static final String AMAP_TRAFFIC_PREFIX = "amap:traffic";

    // 百度AI缓存键前缀
    private static final String BAIDU_SCENE_PREFIX = "baidu:scene";
    private static final String BAIDU_DISH_PREFIX = "baidu:dish";
    private static final String BAIDU_OCR_PREFIX = "baidu:ocr";

    // 缓存过期时间（秒）
    private static final long QA_CACHE_TTL = 3600; // 1小时
    private static final long RECOMMEND_CACHE_TTL = 7200; // 2小时
    private static final long ITINERARY_CACHE_TTL = 86400; // 24小时
    private static final long ATTRACTION_CACHE_TTL = 86400; // 24小时
    private static final long TRANSLATE_CACHE_TTL = 86400; // 24小时
    private static final long CHAT_CACHE_TTL = 1800; // 30分钟
    private static final long IMAGE_CACHE_TTL = 86400; // 24小时

    private static final long WEATHER_CACHE_TTL = 1800; // 30分钟（天气变化较快）
    private static final long ROUTE_CACHE_TTL = 3600; // 1小时
    private static final long PLACE_CACHE_TTL = 86400; // 24小时
    private static final long TRAFFIC_CACHE_TTL = 300; // 5分钟（交通状况变化快）

    /**
     * 获取或设置问答缓存
     */
    public String getOrSetQACache(String question, java.util.function.Supplier<String> loader) {
        String cacheKey = CacheUtil.generateKey(AI_QA_PREFIX, question.hashCode());

        String cached = cacheUtil.get(cacheKey, String.class);
        if (cached != null) {
            log.debug("命中问答缓存: question={}", question);
            return cached;
        }

        String result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, QA_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置问答缓存: question={}, ttl={}s", question, QA_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置推荐缓存
     */
    public Object getOrSetRecommendCache(String userInput, Integer userId,
                                         java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AI_RECOMMEND_PREFIX,
                userInput.hashCode(), userId != null ? userId : "anonymous");

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中推荐缓存: userInput={}", userInput);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, RECOMMEND_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置推荐缓存: userInput={}, ttl={}s", userInput, RECOMMEND_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置行程缓存
     */
    public String getOrSetItineraryCache(String destination, int days, String preferences,
                                         java.util.function.Supplier<String> loader) {
        String cacheKey = CacheUtil.generateKey(AI_ITINERARY_PREFIX,
                destination, days, preferences.hashCode());

        String cached = cacheUtil.get(cacheKey, String.class);
        if (cached != null) {
            log.debug("命中行程缓存: destination={}, days={}", destination, days);
            return cached;
        }

        String result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, ITINERARY_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置行程缓存: destination={}, ttl={}s", destination, ITINERARY_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置景点介绍缓存
     */
    public String getOrSetAttractionIntroCache(Integer attractionId,
                                               java.util.function.Supplier<String> loader) {
        String cacheKey = CacheUtil.generateKey(AI_ATTRACTION_PREFIX, "intro", attractionId);

        String cached = cacheUtil.get(cacheKey, String.class);
        if (cached != null) {
            log.debug("命中景点介绍缓存: attractionId={}", attractionId);
            return cached;
        }

        String result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, ATTRACTION_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置景点介绍缓存: attractionId={}, ttl={}s", attractionId, ATTRACTION_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置翻译缓存
     */
    public String getOrSetTranslateCache(String text, String targetLanguage,
                                         java.util.function.Supplier<String> loader) {
        String cacheKey = CacheUtil.generateKey(AI_TRANSLATE_PREFIX,
                text.hashCode(), targetLanguage);

        String cached = cacheUtil.get(cacheKey, String.class);
        if (cached != null) {
            log.debug("命中翻译缓存: text={}, targetLanguage={}", text, targetLanguage);
            return cached;
        }

        String result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, TRANSLATE_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置翻译缓存: targetLanguage={}, ttl={}s", targetLanguage, TRANSLATE_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置天气缓存
     */
    public Object getOrSetWeatherCache(String cityCode,
                                       java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AMAP_WEATHER_PREFIX, cityCode);

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中天气缓存: cityCode={}", cityCode);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, WEATHER_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置天气缓存: cityCode={}, ttl={}s", cityCode, WEATHER_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置路径规划缓存
     */
    public Object getOrSetRouteCache(double originLng, double originLat,
                                     double destLng, double destLat, String type,
                                     java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AMAP_ROUTE_PREFIX, type,
                String.format("%.6f,%.6f-%.6f,%.6f", originLng, originLat, destLng, destLat));

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中路径规划缓存: type={}", type);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, ROUTE_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置路径规划缓存: type={}, ttl={}s", type, ROUTE_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置地点搜索缓存
     */
    public Object getOrSetPlaceCache(String keywords, String city, int page,
                                     java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AMAP_PLACE_PREFIX, keywords, city, page);

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中地点搜索缓存: keywords={}, city={}", keywords, city);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, PLACE_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置地点搜索缓存: keywords={}, ttl={}s", keywords, PLACE_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置交通状况缓存
     */
    public Object getOrSetTrafficCache(String cityCode,
                                       java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AMAP_TRAFFIC_PREFIX, cityCode);

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中交通状况缓存: cityCode={}", cityCode);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, TRAFFIC_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置交通状况缓存: cityCode={}, ttl={}s", cityCode, TRAFFIC_CACHE_TTL);
        }

        return result;
    }

    /**
     * 获取或设置图像识别缓存
     */
    public Object getOrSetImageCache(String imageHash, String analysisType,
                                     java.util.function.Supplier<Object> loader) {
        String cacheKey = CacheUtil.generateKey(AI_IMAGE_PREFIX, analysisType, imageHash);

        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached != null) {
            log.debug("命中图像识别缓存: type={}", analysisType);
            return cached;
        }

        Object result = loader.get();
        if (result != null) {
            cacheUtil.set(cacheKey, result, IMAGE_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("设置图像识别缓存: type={}, ttl={}s", analysisType, IMAGE_CACHE_TTL);
        }

        return result;
    }

    /**
     * 清除指定类型的缓存
     */
    public void clearCacheByType(String type) {
        String pattern;
        switch (type) {
            case "qa":
                pattern = AI_QA_PREFIX + ":*";
                break;
            case "recommend":
                pattern = AI_RECOMMEND_PREFIX + ":*";
                break;
            case "weather":
                pattern = AMAP_WEATHER_PREFIX + ":*";
                break;
            case "route":
                pattern = AMAP_ROUTE_PREFIX + ":*";
                break;
            case "all":
                pattern = "ai:*";
                break;
            default:
                log.warn("未知的缓存类型: {}", type);
                return;
        }

        cacheUtil.deleteByPattern(pattern);
        log.info("清除缓存完成: type={}, pattern={}", type, pattern);
    }

    /**
     * 清除过期缓存（可定时调用）
     */
    public void clearExpiredCache() {
        log.info("开始清理过期缓存...");
        // Redis会自动处理过期键，这里可以添加额外的清理逻辑
        // 例如：清理大对象、统计缓存命中率等
        log.info("过期缓存清理完成");
    }
}
