package travel.attraction.service.impl;

import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_recommendation.AttractionReview;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.mapper.travel_recommendation_mapper.AttractionReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import travel.attraction.service.AttractionDetailService;
import travel.attraction.service.AttractionService;
import travel.common.vo.CursorPageResult;
import travel.common.utils.CacheUtil;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.security.AuthenticatedUserSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttractionDetailServiceImpl implements AttractionDetailService {

    private static final Logger log = LoggerFactory.getLogger(AttractionDetailServiceImpl.class);

    private final AttractionService attractionService;

    private final CacheUtil cacheUtil;

    private final AttractionReviewMapper attractionReviewMapper;

    private final AttractionRealtimeStatusMapper realtimeStatusMapper;

    private static final String ATTRACTION_DETAIL_PREFIX = "attraction:detail:v2:";
    private static final String CROWD_FORECAST_PREFIX = "crowd:forecast:v2:";
    private static final String HEATMAP_PREFIX = "heatmap:v2:";

    @Override
    public Attraction getAttractionDetail(Long id) {
        validateAttractionId(id);
        log.info("获取景点详情: id={}", id);
        Attraction attraction = attractionService.getById(id.intValue());
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        return attraction;
    }

    @Override
    public Attraction createAttractionDetail(Attraction detail) {
        if (detail == null || detail.getName() == null || detail.getName().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("创建景点详情: name={}", detail.getName());
        if (!attractionService.save(detail)) {
            throw new BusinessException(ErrorCodeEnum.DATABASE_INSERT_ERROR);
        }
        return detail;
    }

    @Override
    public Attraction updateAttractionDetail(Long id, Attraction detail) {
        validateAttractionId(id);
        if (detail == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("更新景点详情: id={}", id);
        detail.setId(id.intValue());
        if (!attractionService.updateById(detail)) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        return detail;
    }

    @Override
    public boolean deleteAttractionDetail(Long id) {
        validateAttractionId(id);
        log.info("删除景点详情: id={}", id);
        if (!attractionService.removeById(id.intValue())) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        return true;
    }

    @Override
    public List<Attraction> getAttractionsByCity(Integer cityId, int page, int size) {
        validatePage(cityId, page, size);
        log.info("按城市查询景点: cityId={}, page={}, size={}", cityId, page, size);
        // Implement pagination with city filter
        return attractionService.lambdaQuery()
                .eq(Attraction::getCityId, cityId)
                .last("LIMIT " + size + " OFFSET " + (page * size))
                .list();
    }

    @Override
    public CursorPageResult<Attraction> getAttractionsByCityCursor(Integer cityId, Integer lastId, int size) {
        if (cityId == null || cityId <= 0 || size <= 0 || size > 100
                || (lastId != null && lastId <= 0)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        List<Attraction> records = attractionService.lambdaQuery()
                .eq(Attraction::getCityId, cityId)
                .lt(lastId != null, Attraction::getId, lastId)
                .orderByDesc(Attraction::getId)
                .last("LIMIT " + (size + 1))
                .list();

        boolean hasMore = records.size() > size;
        if (hasMore) {
            records = new ArrayList<>(records.subList(0, size));
        }

        Integer nextCursor = records.isEmpty() ? null : records.get(records.size() - 1).getId();
        return new CursorPageResult<>(records, nextCursor, hasMore, null, nextCursor);
    }

    @Override
    public List<Attraction> searchAttractions(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank() || keyword.length() > 100
                || page < 0 || page > 1_000_000 || size <= 0 || size > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String normalizedKeyword = keyword.trim();
        log.info("搜索景点: keyword={}, page={}, size={}", keyword, page, size);
        // Implement search with keyword
        return attractionService.lambdaQuery()
                .like(Attraction::getName, normalizedKeyword)
                .or()
                .like(Attraction::getDescription, normalizedKeyword)
                .last("LIMIT " + size + " OFFSET " + (page * size))
                .list();
    }

    @Override
    public List<String> getAttractionImages(Long id) {
        log.info("获取景点图片: id={}", id);
        // Return empty list for now, would query from database
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getAttractionReviews(Long id, int page, int size) {
        if (id == null || id <= 0 || page < 0 || page > 1_000_000 || size <= 0 || size > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("获取景点评论: id={}, page={}, size={}", id, page, size);
        List<AttractionReview> reviews = attractionReviewMapper.selectPage(
                new Page<>((long) page + 1, size),
                new LambdaQueryWrapper<AttractionReview>()
                        .eq(AttractionReview::getAttractionId, id)
                        .orderByDesc(AttractionReview::getUpdatedAt)
                        .orderByDesc(AttractionReview::getId))
                .getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AttractionReview review : reviews) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("attractionId", review.getAttractionId());
            map.put("userId", review.getUserId());
            map.put("rating", review.getRating());
            map.put("content", review.getContent());
            map.put("createTime", review.getCreatedAt() == null ? null : review.getCreatedAt().toString());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveAttractionReview(Integer attractionId, Integer ignoredUserId, Integer rating, String content) {
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        if (attractionId == null || attractionId <= 0 || rating == null || rating < 1 || rating > 5
                || (content != null && content.length() > 2000)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (attractionService.getById(attractionId) == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        log.info("保存景点点评: attractionId={}, userId={}, rating={}", attractionId, currentUserId, rating);
        AttractionReview review = new AttractionReview();
        review.setAttractionId(attractionId);
        review.setUserId(currentUserId);
        review.setCreatedAt(java.time.LocalDateTime.now());
        review.setRating(rating);
        review.setContent(content == null ? "" : content.trim());
        review.setUpdatedAt(java.time.LocalDateTime.now());
        int affectedRows = attractionReviewMapper.upsertReview(review);
        if (affectedRows <= 0) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", review.getId());
        map.put("attractionId", review.getAttractionId());
        map.put("userId", review.getUserId());
        map.put("rating", review.getRating());
        map.put("content", review.getContent());
        map.put("createTime", review.getCreatedAt().toString());
        return map;
    }

    @Override
    public Map<String, Object> getRatingStatistics(Long id) {
        validateAttractionId(id);
        if (attractionService.getById(id.intValue()) == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        log.info("获取景点评分统计: id={}", id);
        List<Map<String, Object>> ratingCounts = attractionReviewMapper.selectRatingCounts(id.intValue());
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            distribution.put(String.valueOf(rating), 0L);
        }
        long totalReviews = 0;
        long weightedRatings = 0;
        for (Map<String, Object> row : ratingCounts) {
            int rating = ((Number) row.get("rating")).intValue();
            long count = ((Number) row.get("rating_count")).longValue();
            if (rating >= 1 && rating <= 5) {
                distribution.put(String.valueOf(rating), count);
                totalReviews += count;
                weightedRatings += (long) rating * count;
            }
        }
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("avgRating", totalReviews == 0 ? 0.0 : (double) weightedRatings / totalReviews);
        statistics.put("totalReviews", totalReviews);
        statistics.put("ratingDistribution", distribution);
        return statistics;
    }

    @Override
    public List<Attraction> getSimilarAttractions(Long id, int limit) {
        validateAttractionId(id);
        if (limit <= 0 || limit > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_RANGE_ERROR);
        }
        log.info("获取相似景点: id={}, limit={}", id, limit);
        // Get current attraction first
        Attraction current = attractionService.getById(id);
        if (current == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        // Find attractions in same city
        return attractionService.lambdaQuery()
                .eq(Attraction::getCityId, current.getCityId())
                .ne(Attraction::getId, id)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public boolean incrementViews(Long id) {
        validateAttractionId(id);
        log.info("增加景点浏览量: id={}", id);
        if (!attractionService.incrementViewCount(id.intValue())) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        return true;
    }

    @Override
    public boolean batchUpdateAttractions(List<Attraction> attractions) {
        if (attractions == null || attractions.isEmpty() || attractions.size() > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("批量更新景点: count={}", attractions.size());
        if (!attractionService.updateBatchById(attractions)) {
            throw new BusinessException(ErrorCodeEnum.DATABASE_UPDATE_ERROR);
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttractionFullDetail(Integer attractionId) {
        String cacheKey = ATTRACTION_DETAIL_PREFIX + attractionId;

        Map<String, Object> cachedDetail = cacheUtil.get(cacheKey, Map.class);
        if (cachedDetail != null) {
            return cachedDetail;
        }

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("basicInfo", extractBasicInfo(attraction));
        detail.put("openingHours", getOpeningHoursDetail(attractionId));
        detail.put("crowdForecast", getCrowdForecast(attractionId, LocalDate.now().toString()));
        detail.put("visitDuration", getRecommendedVisitDuration(attractionId, "general"));
        detail.put("facilities", getAttractionFacilities(attractionId));
        detail.put("bestVisitTime", getBestVisitTime(attractionId));
        detail.put("seasonalInfo", getSeasonalInfo(attractionId));
        detail.put("accessibility", getAccessibilityInfo(attractionId));
        detail.put("photoSpots", getPhotoSpots(attractionId));

        cacheUtil.set(cacheKey, detail, 30, java.util.concurrent.TimeUnit.MINUTES);

        return detail;
    }

    @Override
    public Map<String, Object> getOpeningHoursDetail(Integer attractionId) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> hours = new HashMap<>();
        hours.put("attractionId", attractionId);
        boolean dataAvailable = attraction.getOpeningHours() != null
                && !attraction.getOpeningHours().isBlank();
        hours.put("dataAvailable", dataAvailable);
        hours.put("source", dataAvailable ? "database" : "unavailable");
        hours.put("regularHours", dataAvailable ? Map.of("regular", attraction.getOpeningHours()) : Collections.emptyMap());
        hours.put("specialHours", Collections.emptyMap());
        hours.put("isOpenNow", null);
        hours.put("nextOpenTime", null);
        hours.put("holidaySchedule", Collections.emptyMap());
        hours.put("message", dataAvailable ? "开放状态需以景区当日公告为准" : "暂无开放时间数据");

        return hours;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCrowdForecast(Integer attractionId, String date) {
        String cacheKey = CROWD_FORECAST_PREFIX + attractionId + ":" + date;

        Map<String, Object> cachedForecast = cacheUtil.get(cacheKey, Map.class);
        if (cachedForecast != null) {
            return cachedForecast;
        }

        AttractionRealtimeStatus status = realtimeStatusMapper.selectByAttractionId(attractionId.longValue());
        boolean dataAvailable = status != null
                && (status.getCrowdCount() != null || status.getCrowdLevel() != null);

        Map<String, Object> forecast = new HashMap<>();
        forecast.put("attractionId", attractionId);
        forecast.put("date", date);
        forecast.put("dataAvailable", dataAvailable);
        forecast.put("forecastAvailable", false);
        forecast.put("source", dataAvailable ? "realtime_snapshot" : "unavailable");
        forecast.put("overallLevel", dataAvailable ? status.getCrowdLevel() : null);
        forecast.put("currentVisitorCount", dataAvailable ? status.getCrowdCount() : null);
        forecast.put("updateTime", dataAvailable ? status.getUpdateTime() : null);
        forecast.put("hourlyForecast", Collections.emptyList());
        forecast.put("peakHours", Collections.emptyList());
        forecast.put("quietHours", Collections.emptyList());
        forecast.put("recommendation", "暂无可靠的人流预测数据");

        cacheUtil.set(cacheKey, forecast, 60, java.util.concurrent.TimeUnit.MINUTES);

        return forecast;
    }

    @Override
    public Map<String, Object> getRecommendedVisitDuration(Integer attractionId, String visitorType) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        int baseDuration = calculateBaseDuration(attraction);

        Map<String, Object> duration = new HashMap<>();
        duration.put("attractionId", attractionId);
        duration.put("visitorType", visitorType);
        duration.put("dataAvailable", true);
        duration.put("source", "description_heuristic");
        duration.put("estimated", true);
        duration.put("minDuration", baseDuration / 2);
        duration.put("recommendedDuration", baseDuration);
        duration.put("maxDuration", baseDuration * 2);
        duration.put("breakdown", generateDurationBreakdown(baseDuration));

        return duration;
    }

    @Override
    public List<Map<String, Object>> getAttractionFacilities(Integer attractionId) {
        return Collections.emptyList();
    }

        @Override
    public List<Map<String, Object>> getNearbyAttractions(Integer id, int limit) {
        log.info("获取周边景点: id={}, limit={}", id, limit);
        Attraction current = attractionService.getById(id);
        if (current == null) {
            return new ArrayList<>();
        }
        int finalLimit = Math.max(1, Math.min(limit, 50));
        double lat = current.getLatitude() == null ? 0.0 : current.getLatitude().doubleValue();
        double lng = current.getLongitude() == null ? 0.0 : current.getLongitude().doubleValue();

        List<Map<String, Object>> result = attractionService.getByCityId(current.getCityId()).stream()
                .filter(a -> !a.getId().equals(id))           // 排除自身
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("name", a.getName());
                    map.put("description", a.getDescription());
                    map.put("address", a.getAddress());
                    double aLat = a.getLatitude() == null ? 0.0 : a.getLatitude().doubleValue();
                    double aLng = a.getLongitude() == null ? 0.0 : a.getLongitude().doubleValue();
                    map.put("distance", calculateDistance(lat, lng, aLat, aLng));
                    return map;
                })
                .sorted((m1, m2) -> Double.compare((Double) m1.get("distance"), (Double) m2.get("distance")))
                .limit(finalLimit)
                .collect(java.util.stream.Collectors.toList());
        return result;
    }

    /** 简化的距离计算（单位：公里） */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0.0 && lng1 == 0.0) {
            return Double.MAX_VALUE;
        }
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double a = sinLat * sinLat + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLng * sinLng;
        return 2 * R * Math.asin(Math.sqrt(a));
    }


    @Override
    public List<Map<String, Object>> getNearbyServices(Integer attractionId, String serviceType) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getBestVisitTime(Integer attractionId) {
        Map<String, Object> bestTime = new HashMap<>();

        bestTime.put("attractionId", attractionId);
        bestTime.put("dataAvailable", false);
        bestTime.put("bestSeason", null);
        bestTime.put("bestTimeOfDay", null);
        bestTime.put("bestWeekday", null);
        bestTime.put("avoidTime", Collections.emptyList());
        bestTime.put("specialEvents", Collections.emptyList());
        bestTime.put("message", "暂无可靠的最佳游览时间数据");

        return bestTime;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCrowdHeatmapData(Integer attractionId) {
        String cacheKey = HEATMAP_PREFIX + attractionId;

        Map<String, Object> cachedHeatmap = cacheUtil.get(cacheKey, Map.class);
        if (cachedHeatmap != null) {
            return cachedHeatmap;
        }

        AttractionRealtimeStatus status = realtimeStatusMapper.selectByAttractionId(attractionId.longValue());
        boolean dataAvailable = status != null && status.getCrowdCount() != null;
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        if (dataAvailable) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", status.getUpdateTime());
            point.put("value", status.getCrowdCount());
            dataPoints.add(point);
        }

        Map<String, Object> heatmap = new HashMap<>();
        heatmap.put("attractionId", attractionId);
        heatmap.put("dataAvailable", dataAvailable);
        heatmap.put("source", dataAvailable ? "realtime_snapshot" : "unavailable");
        heatmap.put("dataPoints", dataPoints);
        heatmap.put("maxValue", dataAvailable ? status.getCrowdCount() : null);
        heatmap.put("timeRange", null);
        heatmap.put("updateTime", dataAvailable ? status.getUpdateTime() : null);

        cacheUtil.set(cacheKey, heatmap, 15, java.util.concurrent.TimeUnit.MINUTES);

        return heatmap;
    }

    @Override
    public Map<String, Object> getSeasonalInfo(Integer attractionId) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> seasonal = new HashMap<>();
        seasonal.put("attractionId", attractionId);
        seasonal.put("dataAvailable", false);
        seasonal.put("source", "unavailable");
        seasonal.put("bestSeasons", Collections.emptyList());
        seasonal.put("seasonalFeatures", Collections.emptyMap());
        seasonal.put("weatherConsiderations", Collections.emptyList());
        seasonal.put("specialEvents", Collections.emptyList());
        seasonal.put("message", "暂无景点季节性资料");

        return seasonal;
    }

    @Override
    public Map<String, Object> getAccessibilityInfo(Integer attractionId) {
        Map<String, Object> accessibility = new HashMap<>();
        accessibility.put("attractionId", attractionId);
        accessibility.put("dataAvailable", false);
        accessibility.put("source", "unavailable");
        accessibility.put("wheelchairAccessible", null);
        accessibility.put("accessibleParking", null);
        accessibility.put("accessibleRestroom", null);
        accessibility.put("accessiblePath", null);
        accessibility.put("elevatorAvailable", null);
        accessibility.put("accessibleSeating", null);
        accessibility.put("serviceAnimalAllowed", null);
        accessibility.put("accessibleTransport", null);
        accessibility.put("specialServices", Collections.emptyList());
        accessibility.put("message", "暂无可靠的无障碍设施资料，请出行前向景区确认");

        return accessibility;
    }

    @Override
    public List<Map<String, Object>> getPhotoSpots(Integer attractionId) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getHistoricalCrowdData(Integer attractionId, Integer days) {
        Map<String, Object> historical = new HashMap<>();

        historical.put("attractionId", attractionId);
        historical.put("days", days);
        historical.put("dataAvailable", false);
        historical.put("averageDailyVisitors", null);
        historical.put("peakDay", Collections.emptyMap());
        historical.put("quietestDay", Collections.emptyMap());
        historical.put("weeklyPattern", Collections.emptyMap());
        historical.put("monthlyTrend", Collections.emptyList());
        historical.put("message", "当前仅保存最新快照，暂无历史人流数据");

        return historical;
    }

    private void validateAttractionId(Long id) {
        if (id == null || id <= 0 || id > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validatePage(Integer cityId, int page, int size) {
        if (cityId == null || cityId <= 0 || page < 0 || page > 1_000_000
                || size <= 0 || size > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    // 辅助方法
    private Map<String, Object> extractBasicInfo(Attraction attraction) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", attraction.getId());
        info.put("name", attraction.getName());
        info.put("description", attraction.getDescription());
        info.put("address", attraction.getAddress());
        info.put("ticketPrice", attraction.getTicketPrice());
        info.put("rating", attraction.getRating());
        info.put("viewCount", attraction.getViewCount());
        info.put("latitude", attraction.getLatitude());
        info.put("longitude", attraction.getLongitude());
        return info;
    }

    private int calculateBaseDuration(Attraction attraction) {
        String description = attraction.getDescription();
        if (description == null) return 120;

        if (description.contains("博物馆") || description.contains("故宫")) {
            return 180;
        } else if (description.contains("公园") || description.contains("山")) {
            return 240;
        } else if (description.contains("塔") || description.contains("建筑")) {
            return 90;
        }
        return 120;
    }

    private Map<String, Integer> generateDurationBreakdown(int baseDuration) {
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("sightseeing", (int) (baseDuration * 0.6));
        breakdown.put("walking", (int) (baseDuration * 0.2));
        breakdown.put("resting", (int) (baseDuration * 0.1));
        breakdown.put("photography", (int) (baseDuration * 0.1));
        return breakdown;
    }

}
