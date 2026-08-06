package travel.attraction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.attraction.service.AttractionBloomFilterService;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.attraction.service.AttractionService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import travel.common.vo.CursorPageResult;
import java.math.BigDecimal;


@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction> implements AttractionService {

    private final CacheUtil cacheUtil;
    private final AttractionBloomFilterService attractionBloomFilterService;

    @Override
    public List<Attraction> list() {
        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "all");
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("Loaded attraction list from cache");
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        List<Attraction> attractions = super.list();
        cacheUtil.set(cacheKey, attractions, 2, TimeUnit.HOURS);
        log.debug("Loaded attraction list from database and cached it");
        return attractions;
    }

    @Override
    public Attraction getById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }

        if (!attractionBloomFilterService.mightContain(id)) {
            log.debug("Bloom filter rejected attraction lookup: id={}", id);
            return null;
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "detail", id);
        String nullCacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "null", id);

        if (cacheUtil.exists(nullCacheKey)) {
            log.debug("Null marker hit for attraction detail: id={}", id);
            return null;
        }

        Attraction cached = cacheUtil.get(cacheKey, Attraction.class);

        if (cached != null) {
            log.debug("Loaded attraction detail from cache: id={}", id);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        Attraction attraction = super.getById(id);
        if (attraction != null) {
            cacheUtil.set(cacheKey, attraction, 1, TimeUnit.HOURS);
            cacheUtil.delete(nullCacheKey);
            log.debug("Cached attraction detail: id={}", id);
        } else {
            cacheUtil.set(nullCacheKey, Boolean.TRUE, 5, TimeUnit.MINUTES);
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
            log.debug("Loaded city attractions from cache: cityId={}", cityId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId).orderByDesc("rating");
        List<Attraction> attractions = baseMapper.selectList(queryWrapper);

        cacheUtil.set(cacheKey, attractions, 1, TimeUnit.HOURS);
        log.debug("Cached city attractions: cityId={}, count={}", cityId, attractions.size());
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
            log.debug("Loaded city/type attractions from cache: cityId={}, type={}", cityId, type);
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
        log.debug("Cached city/type attractions: cityId={}, type={}, count={}", cityId, type, attractions.size());
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
            log.debug("Loaded attraction from mapper cache: id={}", attractionId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        Attraction attraction = baseMapper.selectById(attractionId);
        if (attraction != null) {
            cacheUtil.set(cacheKey, attraction, 1, TimeUnit.HOURS);
            log.debug("Cached attraction from mapper lookup: id={}", attractionId);
        }
        return attraction;
    }

    @Override
    public List<Attraction> getEnableAndSyncOpenAttractions() {
        String cacheKey = CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "enabled_sync_open");
        List<Attraction> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("Loaded enabled and synced-open attractions from cache");
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
        log.debug("Cached enabled and synced-open attractions: count={}", attractions.size());
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
            log.debug("Loaded top-rated attractions from cache: cityId={}", cityId);
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
        log.debug("Cached top-rated attractions: cityId={}, count={}", cityId, attractions.size());
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
            log.debug("浠庣紦瀛樿幏鍙栨悳绱㈢粨鏋?Map): cityId={}, keyword={}", cityId, keyword);
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
        log.debug("Cached search result map: cityId={}, keyword={}, count={}", cityId, keyword, result.size());
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
            log.debug("浠庣紦瀛樿幏鍙栨悳绱㈢粨鏋? keyword={}", keyword);
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
        log.debug("Cached search results: keyword={}, count={}", keyword, attractions.size());
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
            log.debug("Loaded recommended attractions from cache: cityId={}", cityId);
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
        log.debug("Cached recommended attractions: cityId={}, count={}", cityId, attractions.size());
        return attractions;
    }

    @Override
    public boolean save(Attraction attraction) {
        boolean result = super.save(attraction);
        if (result) {
            invalidateAttractionCache(attraction.getCityId());
            attractionBloomFilterService.put(attraction.getId());
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "null", attraction.getId()));
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
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "null", attraction.getId()));
            attractionBloomFilterService.put(attraction.getId());
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
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ATTRACTION_KEY_PREFIX, "null", id));
            attractionBloomFilterService.refreshBloomFilter();
        }
        return result;
    }


    @Override
    public CursorPageResult<Attraction> getByCursor(Integer cityId, String cursor, int size) {
        if (cityId == null || cityId <= 0) {
            return CursorPageResult.empty();
        }
        if (size <= 0 || size > 100) {
            size = 10;
        }

        BigDecimal lastRating = null;
        Integer lastId = null;
        BigDecimal[] decoded = CursorPageResult.decodeCursor(cursor);
        if (decoded != null) {
            lastRating = decoded[0];
            lastId = decoded[1].intValue();
        }

        List<Attraction> records = baseMapper.selectByCursor(cityId, lastRating, lastId, size + 1);
        boolean hasMore = records.size() > size;
        if (hasMore) {
            records = records.subList(0, size);
        }

        BigDecimal newLastRating = null;
        Integer newLastId = null;
        if (!records.isEmpty()) {
            Attraction last = records.get(records.size() - 1);
            newLastRating = last.getRating();
            newLastId = last.getId();
        }

        return CursorPageResult.of(records, hasMore, newLastRating, newLastId);
    }

    @Override
    public CursorPageResult<Attraction> getAllByCursor(String cursor, int size) {
        if (size <= 0 || size > 100) {
            size = 10;
        }

        BigDecimal lastRating = null;
        Integer lastId = null;
        BigDecimal[] decoded = CursorPageResult.decodeCursor(cursor);
        if (decoded != null) {
            lastRating = decoded[0];
            lastId = decoded[1].intValue();
        }

        List<Attraction> records = baseMapper.selectAllByCursor(lastRating, lastId, size + 1);
        boolean hasMore = records.size() > size;
        if (hasMore) {
            records = records.subList(0, size);
        }

        BigDecimal newLastRating = null;
        Integer newLastId = null;
        if (!records.isEmpty()) {
            Attraction last = records.get(records.size() - 1);
            newLastRating = last.getRating();
            newLastId = last.getId();
        }

        return CursorPageResult.of(records, hasMore, newLastRating, newLastId);
    }

    @Override
    public Map<String, Object> comparePagination(Integer cityId, int page, int size) {
        if (cityId == null || cityId <= 0) {
            return Map.of("error", "cityId is required");
        }
        if (page <= 0) page = 1;
        if (size <= 0 || size > 100) size = 10;

        Map<String, Object> result = new java.util.LinkedHashMap<>();

        long offsetStart = System.nanoTime();
        int offset = (page - 1) * size;
        List<Attraction> offsetRecords = baseMapper.selectByOffset(cityId, offset, size);
        long offsetDuration = System.nanoTime() - offsetStart;

        long cursorStart = System.nanoTime();
        BigDecimal lastRating = null;
        Integer lastId = null;
        if (page > 1) {
            List<Attraction> prevPage = baseMapper.selectByOffset(cityId, offset - size, size);
            if (!prevPage.isEmpty()) {
                Attraction prevLast = prevPage.get(prevPage.size() - 1);
                lastRating = prevLast.getRating();
                lastId = prevLast.getId();
            }
        }
        List<Attraction> cursorRecords = baseMapper.selectByCursor(cityId, lastRating, lastId, size);
        long cursorDuration = System.nanoTime() - cursorStart;

        result.put("cityId", cityId);
        result.put("page", page);
        result.put("size", size);
        result.put("offsetPagination", Map.of(
                "records", offsetRecords,
                "durationNanos", offsetDuration,
                "durationMs", String.format("%.3f", offsetDuration / 1_000_000.0)
        ));
        result.put("cursorPagination", Map.of(
                "records", cursorRecords,
                "durationNanos", cursorDuration,
                "durationMs", String.format("%.3f", cursorDuration / 1_000_000.0)
        ));
        result.put("conclusion", Map.of(
                "offsetDescription", "OFFSET pagination uses LIMIT offset, size and slows down as pages get deeper",
                "cursorDescription", "Cursor pagination uses WHERE (rating, id) < (lastRating, lastId) and stays stable on deep pages",
                "coveringIndexDescription", "Covering index idx_city_rating_cover(city_id, rating DESC, name, id) helps avoid table lookups",
                "recommendation", "Use cursor pagination together with a covering index for deep pagination"
        ));

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
        log.info("Invalidated attraction cache: cityId={}", cityId);
    }
}




