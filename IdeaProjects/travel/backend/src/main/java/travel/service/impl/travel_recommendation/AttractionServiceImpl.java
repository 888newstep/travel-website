package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.Attraction;
import travel.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction> implements AttractionService {

    private final CacheUtil cacheUtil;

    @Override
    public List<Attraction> list() {
        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "all");
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取景点列表");
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        List<Attraction> attractions = super.list();
        cacheUtil.set(cacheKey, attractions, 2, TimeUnit.HOURS);
        log.debug("从数据库获取景点列表并缓存");
        return attractions;
    }

    @Override
    public Attraction getById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "detail", id);
        Attraction cached = cacheUtil.get(cacheKey, Attraction.class);

        if (cached != null) {
            log.debug("从缓存获取景点详情: id={}", id);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        Attraction attraction = super.getById(id);
        if (attraction != null) {
            cacheUtil.set(cacheKey, attraction, 1, TimeUnit.HOURS);
            log.debug("缓存景点详情: id={}", id);
        }
        return attraction;
    }

    @Override
    public List<Attraction> getByCityId(Integer cityId) {
        if (cityId == null || cityId <= 0) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "city", cityId);
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取城市景点: cityId={}", cityId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId).orderByDesc("rating");
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 1, TimeUnit.HOURS);
        log.debug("缓存城市景点: cityId={}, count={}", cityId, attractions.size());
        return attractions;
    }

    @Override
    public List<Attraction> getByCityIdAndType(Integer cityId, String type) {
        if (cityId == null || cityId <= 0 || type == null || type.isBlank()) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "city_type", cityId, type);
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取城市类型景点: cityId={}, type={}", cityId, type);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId)
                .eq("type", type)
                .orderByDesc("rating");
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 1, TimeUnit.HOURS);
        log.debug("缓存城市类型景点: cityId={}, type={}, count={}", cityId, type, attractions.size());
        return attractions;
    }

    @Override
    public Attraction getByIdUsingMapper(Integer attractionId) {
        if (attractionId == null || attractionId <= 0) {
            return null;
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "mapper", attractionId);
        Attraction cached = cacheUtil.get(cacheKey, Attraction.class);

        if (cached != null) {
            log.debug("从缓存获取景点(Mapper): id={}", attractionId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        Attraction attraction = baseMapper.selectById(attractionId);
        if (attraction != null) {
            cacheUtil.set(cacheKey, attraction, 1, TimeUnit.HOURS);
            log.debug("缓存景点(Mapper): id={}", attractionId);
        }
        return attraction;
    }

    @Override
    public List<Attraction> getEnableAndSyncOpenAttractions() {
        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "enabled_sync_open");
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取启用且同步开放的景点");
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", true)
                .eq("sync_open_status", true)
                .orderByDesc("rating");
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 30, TimeUnit.MINUTES);
        log.debug("缓存启用且同步开放的景点: count={}", attractions.size());
        return attractions;
    }

    @Override
    public List<Attraction> getTopRated(Integer cityId, int limit) {
        if (cityId == null || cityId <= 0) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "top_rated", cityId, limit);
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取高评分景点: cityId={}", cityId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId)
                .orderByDesc("rating")
                .last("LIMIT " + limit);
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 2, TimeUnit.HOURS);
        log.debug("缓存高评分景点: cityId={}, count={}", cityId, attractions.size());
        return attractions;
    }

    @Override
    public List<Map<String, Object>> search(Integer cityId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "search_map", cityId, keyword);
        List<Map<String, Object>> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取搜索结果(Map): cityId={}, keyword={}", cityId, keyword);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        if (cityId != null && cityId > 0) {
            queryWrapper.eq("city_id", cityId);
        }
        queryWrapper.and(wrapper -> wrapper
                        .like("name", keyword)
                        .or()
                        .like("description", keyword))
                .orderByDesc("rating");

        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        List<Map<String, Object>> result = attractions.stream().map(attraction -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", attraction.getId());
            map.put("name", attraction.getName());
            map.put("cityId", attraction.getCityId());
            map.put("rating", attraction.getRating());
            map.put("description", attraction.getDescription());
            return map;
        }).collect(Collectors.toList());

        cacheUtil.set(cacheKey, result, 30, TimeUnit.MINUTES);
        log.debug("缓存搜索结果(Map): cityId={}, keyword={}, count={}", cityId, keyword, result.size());
        return result;
    }

    @Override
    public List<Attraction> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "search", keyword);
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取搜索结果: keyword={}", keyword);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                .or()
                .like("description", keyword)
                .orderByDesc("rating");
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 30, TimeUnit.MINUTES);
        log.debug("缓存搜索结果: keyword={}, count={}", keyword, attractions.size());
        return attractions;
    }

    @Override
    public List<Attraction> getRecommendations(Integer cityId, int limit) {
        if (cityId == null || cityId <= 0) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "recommend", cityId, limit);
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取推荐景点: cityId={}", cityId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId)
                .orderByDesc("rating")
                .last("LIMIT " + limit);
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 2, TimeUnit.HOURS);
        log.debug("缓存推荐景点: cityId={}, count={}", cityId, attractions.size());
        return attractions;
    }

    @Override
    public boolean save(Attraction attraction) {
        boolean result = super.save(attraction);
        if (result) {
            invalidateAttractionCache(attraction.getCityId());
        }
        return result;
    }

    @Override
    public boolean updateById(Attraction attraction) {
        boolean result = super.updateById(attraction);
        if (result) {
            invalidateAttractionCache(attraction.getCityId());
            String detailKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "detail", attraction.getId());
            cacheUtil.delete(detailKey);
        }
        return result;
    }

    @Override
    public boolean removeById(Integer id) {
        Attraction attraction = getById(id);
        boolean result = super.removeById(id);
        if (result && attraction != null) {
            invalidateAttractionCache(attraction.getCityId());
            String detailKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "detail", id);
            cacheUtil.delete(detailKey);
        }
        return result;
    }

    private void invalidateAttractionCache(Integer cityId) {
        cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "all"));
        if (cityId != null) {
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "city", cityId));
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "recommend", cityId));
            cacheUtil.deleteByPattern(CacheUtil.ATTRACTION_KEY_PREFIX + ":city_type:" + cityId + ":*");
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "top_rated", cityId));
        }
        cacheUtil.deleteByPattern(CacheUtil.ATTRACTION_KEY_PREFIX + ":search:*");
        cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "enabled_sync_open"));
        log.info("景点缓存已失效: cityId={}", cityId);
    }
}
