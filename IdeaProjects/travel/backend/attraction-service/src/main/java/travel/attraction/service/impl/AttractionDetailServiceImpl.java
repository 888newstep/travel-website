package travel.attraction.service.impl;

import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_recommendation.Attraction;
import travel.attraction.service.AttractionDetailService;
import travel.attraction.service.AttractionService;
import travel.common.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttractionDetailServiceImpl implements AttractionDetailService {

    private static final Logger log = LoggerFactory.getLogger(AttractionDetailServiceImpl.class);

    private final AttractionService attractionService;

    private final CacheUtil cacheUtil;

    private static final String ATTRACTION_DETAIL_PREFIX = "attraction:detail:";
    private static final String CROWD_FORECAST_PREFIX = "crowd:forecast:";
    private static final String HEATMAP_PREFIX = "heatmap:";

    @Override
    public Attraction getAttractionDetail(Long id) {
        log.info("获取景点详情: id={}", id);
        return attractionService.getById(id);
    }

    @Override
    public Attraction createAttractionDetail(Attraction detail) {
        log.info("创建景点详情: name={}", detail.getName());
        attractionService.save(detail);
        return detail;
    }

    @Override
    public Attraction updateAttractionDetail(Long id, Attraction detail) {
        log.info("更新景点详情: id={}", id);
        detail.setId(id.intValue());
        attractionService.updateById(detail);
        return detail;
    }

    @Override
    public boolean deleteAttractionDetail(Long id) {
        log.info("删除景点详情: id={}", id);
        return attractionService.removeById(id);
    }

    @Override
    public List<Attraction> getAttractionsByCity(Integer cityId, int page, int size) {
        log.info("按城市查询景点: cityId={}, page={}, size={}", cityId, page, size);
        // Implement pagination with city filter
        return attractionService.lambdaQuery()
                .eq(Attraction::getCityId, cityId)
                .last("LIMIT " + size + " OFFSET " + (page * size))
                .list();
    }

    @Override
    public List<Attraction> searchAttractions(String keyword, int page, int size) {
        log.info("搜索景点: keyword={}, page={}, size={}", keyword, page, size);
        // Implement search with keyword
        return attractionService.lambdaQuery()
                .like(Attraction::getName, keyword)
                .or()
                .like(Attraction::getDescription, keyword)
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
        log.info("获取景点评论: id={}, page={}, size={}", id, page, size);
        // Return empty list for now, would query from database
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getRatingStatistics(Long id) {
        log.info("获取景点评分统计: id={}", id);
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("avgRating", 4.5);
        statistics.put("totalReviews", 100);
        statistics.put("ratingDistribution", Map.of(
                "5", 50,
                "4", 30,
                "3", 15,
                "2", 4,
                "1", 1
        ));
        return statistics;
    }

    @Override
    public List<Attraction> getSimilarAttractions(Long id, int limit) {
        log.info("获取相似景点: id={}, limit={}", id, limit);
        // Get current attraction first
        Attraction current = attractionService.getById(id);
        if (current == null) {
            return Collections.emptyList();
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
        log.info("增加景点浏览量: id={}", id);
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            attraction.setViewCount(attraction.getViewCount() + 1);
            return attractionService.updateById(attraction);
        }
        return false;
    }

    @Override
    public boolean batchUpdateAttractions(List<Attraction> attractions) {
        log.info("批量更新景点: count={}", attractions.size());
        return attractionService.updateBatchById(attractions);
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
        hours.put("regularHours", parseOpeningHours(attraction.getOpeningHours()));
        hours.put("specialHours", generateSpecialHours());
        hours.put("isOpenNow", isOpenNow(attraction.getOpeningHours()));
        hours.put("nextOpenTime", calculateNextOpenTime(attraction.getOpeningHours()));
        hours.put("holidaySchedule", generateHolidaySchedule());

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

        Map<String, Object> forecast = new HashMap<>();
        forecast.put("attractionId", attractionId);
        forecast.put("date", date);
        forecast.put("overallLevel", new Random().nextInt(5) + 1);
        forecast.put("hourlyForecast", generateHourlyForecast());
        forecast.put("peakHours", generatePeakHours());
        forecast.put("quietHours", generateQuietHours());
        forecast.put("recommendation", generateCrowdRecommendation());

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
        duration.put("minDuration", baseDuration / 2);
        duration.put("recommendedDuration", baseDuration);
        duration.put("maxDuration", baseDuration * 2);
        duration.put("breakdown", generateDurationBreakdown(baseDuration));

        return duration;
    }

    @Override
    public List<Map<String, Object>> getAttractionFacilities(Integer attractionId) {
        List<Map<String, Object>> facilities = new ArrayList<>();

        facilities.add(createFacility("restroom", "洗手间", true, "主入口右侧"));
        facilities.add(createFacility("parking", "停车场", true, "景区东侧，500个车位"));
        facilities.add(createFacility("restaurant", "餐厅", true, "景区内3处"));
        facilities.add(createFacility("shop", "纪念品店", true, "出口处"));
        facilities.add(createFacility("wifi", "免费WiFi", true, "全景区覆盖"));
        facilities.add(createFacility("locker", "储物柜", true, "游客中心"));
        facilities.add(createFacility("firstAid", "急救站", true, "游客中心旁"));
        facilities.add(createFacility("wheelchair", "轮椅租赁", true, "需要押金"));

        return facilities;
    }

    @Override
    public List<Map<String, Object>> getNearbyServices(Integer attractionId, String serviceType) {
        List<Map<String, Object>> services = new ArrayList<>();
        Attraction attraction = attractionService.getById(attractionId);

        if (attraction == null) {
            return services;
        }

        // 模拟周边服务数据
        switch (serviceType) {
            case "restaurant":
                services.add(createNearbyService("老北京炸酱面", "餐厅", 0.3, "中式快餐", 4.5));
                services.add(createNearbyService("星巴克", "咖啡厅", 0.5, "西式咖啡", 4.7));
                services.add(createNearbyService("必胜客", "餐厅", 0.8, "西式快餐", 4.3));
                break;
            case "parking":
                services.add(createNearbyService("景区停车场", "停车场", 0.1, "大型停车场", 4.0));
                services.add(createNearbyService("商业区停车场", "停车场", 0.5, "地下停车场", 4.2));
                break;
            case "restroom":
                services.add(createNearbyService("景区公厕", "公共厕所", 0.05, "免费", 3.8));
                services.add(createNearbyService("商场洗手间", "洗手间", 0.4, "商场内", 4.5));
                break;
            case "shop":
                services.add(createNearbyService("便利店", "便利店", 0.2, "24小时营业", 4.4));
                services.add(createNearbyService("特产店", "特产店", 0.3, "当地特产", 4.1));
                break;
        }

        return services;
    }

    @Override
    public Map<String, Object> getBestVisitTime(Integer attractionId) {
        Map<String, Object> bestTime = new HashMap<>();

        bestTime.put("attractionId", attractionId);
        bestTime.put("bestSeason", generateBestSeason());
        bestTime.put("bestTimeOfDay", generateBestTimeOfDay());
        bestTime.put("bestWeekday", generateBestWeekday());
        bestTime.put("avoidTime", generateAvoidTime());
        bestTime.put("specialEvents", generateSpecialEvents());

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

        Map<String, Object> heatmap = new HashMap<>();
        heatmap.put("attractionId", attractionId);
        heatmap.put("dataPoints", generateHeatmapDataPoints());
        heatmap.put("maxValue", 100);
        heatmap.put("timeRange", "08:00-22:00");
        heatmap.put("updateTime", new Date());

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
        seasonal.put("bestSeasons", generateBestSeasons(attraction));
        seasonal.put("seasonalFeatures", generateSeasonalFeatures());
        seasonal.put("weatherConsiderations", generateWeatherConsiderations());
        seasonal.put("specialEvents", generateSeasonalEvents());

        return seasonal;
    }

    @Override
    public Map<String, Object> getAccessibilityInfo(Integer attractionId) {
        Map<String, Object> accessibility = new HashMap<>();

        accessibility.put("attractionId", attractionId);
        accessibility.put("wheelchairAccessible", true);
        accessibility.put("accessibleParking", true);
        accessibility.put("accessibleRestroom", true);
        accessibility.put("accessiblePath", true);
        accessibility.put("elevatorAvailable", true);
        accessibility.put("accessibleSeating", true);
        accessibility.put("serviceAnimalAllowed", true);
        accessibility.put("accessibleTransport", true);
        accessibility.put("specialServices", generateSpecialServices());

        return accessibility;
    }

    @Override
    public List<Map<String, Object>> getPhotoSpots(Integer attractionId) {
        List<Map<String, Object>> spots = new ArrayList<>();

        spots.add(createPhotoSpot("主入口", "标志性建筑", "全天", 4.8));
        spots.add(createPhotoSpot("观景台", "全景视野", "上午", 4.9));
        spots.add(createPhotoSpot("花园广场", "花海背景", "下午", 4.6));
        spots.add(createPhotoSpot("湖边栈道", "水景倒影", "傍晚", 4.7));
        spots.add(createPhotoSpot("古建筑群", "历史韵味", "上午", 4.5));

        return spots;
    }

    @Override
    public Map<String, Object> getHistoricalCrowdData(Integer attractionId, Integer days) {
        Map<String, Object> historical = new HashMap<>();

        historical.put("attractionId", attractionId);
        historical.put("days", days);
        historical.put("averageDailyVisitors", 5000 + new Random().nextInt(5000));
        historical.put("peakDay", generatePeakDay());
        historical.put("quietestDay", generateQuietestDay());
        historical.put("weeklyPattern", generateWeeklyPattern());
        historical.put("monthlyTrend", generateMonthlyTrend());

        return historical;
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

    private Map<String, Object> parseOpeningHours(String openingHoursStr) {
        Map<String, Object> hours = new HashMap<>();

        if (openingHoursStr == null || openingHoursStr.isEmpty()) {
            hours.put("monday", "08:00-18:00");
            hours.put("tuesday", "08:00-18:00");
            hours.put("wednesday", "08:00-18:00");
            hours.put("thursday", "08:00-18:00");
            hours.put("friday", "08:00-18:00");
            hours.put("saturday", "08:00-20:00");
            hours.put("sunday", "08:00-20:00");
        } else {
            // 解析实际的开放时间字符串
            hours.put("regular", openingHoursStr);
        }

        return hours;
    }

    private Map<String, Object> generateSpecialHours() {
        Map<String, Object> special = new HashMap<>();
        special.put("hasSpecialHours", false);
        special.put("notes", "无特殊开放时间");
        return special;
    }

    private boolean isOpenNow(String openingHours) {
        // 简化判断，实际应该解析时间
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(18, 0));
    }

    private String calculateNextOpenTime(String openingHours) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(8, 0))) {
            return "今天 08:00";
        } else if (now.isAfter(LocalTime.of(18, 0))) {
            return "明天 08:00";
        }
        return "已开放";
    }

    private Map<String, Object> generateHolidaySchedule() {
        Map<String, Object> holiday = new HashMap<>();
        holiday.put("springFestival", "09:00-17:00");
        holiday.put("nationalDay", "07:00-21:00");
        holiday.put("laborDay", "08:00-20:00");
        return holiday;
    }

    private List<Map<String, Object>> generateHourlyForecast() {
        List<Map<String, Object>> hourly = new ArrayList<>();

        for (int hour = 8; hour <= 20; hour++) {
            Map<String, Object> data = new HashMap<>();
            data.put("hour", String.format("%02d:00", hour));
            data.put("crowdLevel", new Random().nextInt(5) + 1);
            data.put("visitorCount", 100 + new Random().nextInt(400));
            hourly.add(data);
        }

        return hourly;
    }

    private List<String> generatePeakHours() {
        return Arrays.asList("10:00-12:00", "14:00-16:00");
    }

    private List<String> generateQuietHours() {
        return Arrays.asList("08:00-09:00", "17:00-18:00");
    }

    private String generateCrowdRecommendation() {
        String[] recommendations = {
            "建议早上8点入园，避开人流高峰",
            "下午2点后人流相对较少",
            "工作日游览体验更佳",
            "节假日建议提前预约"
        };
        return recommendations[new Random().nextInt(recommendations.length)];
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

    private Map<String, Object> createFacility(String code, String name, boolean available, String location) {
        Map<String, Object> facility = new HashMap<>();
        facility.put("code", code);
        facility.put("name", name);
        facility.put("available", available);
        facility.put("location", location);
        return facility;
    }

    private Map<String, Object> createNearbyService(String name, String type, double distance, String description, double rating) {
        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("type", type);
        service.put("distance", distance);
        service.put("description", description);
        service.put("rating", rating);
        return service;
    }

    private String generateBestSeason() {
        String[] seasons = {"春季（3-5月）", "秋季（9-11月）", "全年皆宜"};
        return seasons[new Random().nextInt(seasons.length)];
    }

    private String generateBestTimeOfDay() {
        String[] times = {"上午8:00-10:00", "下午14:00-16:00", "傍晚17:00-19:00"};
        return times[new Random().nextInt(times.length)];
    }

    private String generateBestWeekday() {
        String[] days = {"周二至周四", "工作日", "避开周末"};
        return days[new Random().nextInt(days.length)];
    }

    private List<String> generateAvoidTime() {
        return Arrays.asList("节假日", "周末上午", "重大活动期间");
    }

    private List<Map<String, Object>> generateSpecialEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        // 模拟特殊活动
        return events;
    }

    private List<Map<String, Object>> generateHeatmapDataPoints() {
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", String.format("%02d:00", i));
            point.put("value", new Random().nextInt(100));
            points.add(point);
        }

        return points;
    }

    private List<String> generateBestSeasons(Attraction attraction) {
        return Arrays.asList("春季", "秋季");
    }

    private Map<String, Object> generateSeasonalFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("spring", "花开时节，景色宜人");
        features.put("summer", "绿树成荫，清凉避暑");
        features.put("autumn", "层林尽染，秋色迷人");
        features.put("winter", "银装素裹，别有一番风味");
        return features;
    }

    private List<String> generateWeatherConsiderations() {
        return Arrays.asList("雨天路滑，注意安全", "夏季注意防晒", "冬季注意保暖");
    }

    private List<Map<String, Object>> generateSeasonalEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        // 模拟季节性活动
        return events;
    }

    private List<Map<String, Object>> generateSpecialServices() {
        List<Map<String, Object>> services = new ArrayList<>();
        services.add(createSpecialService("轮椅租赁", "免费提供"));
        services.add(createSpecialService("无障碍导游", "需预约"));
        services.add(createSpecialService("手语服务", "周末提供"));
        return services;
    }

    private Map<String, Object> createSpecialService(String name, String note) {
        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("note", note);
        return service;
    }

    private Map<String, Object> createPhotoSpot(String name, String feature, String bestTime, double rating) {
        Map<String, Object> spot = new HashMap<>();
        spot.put("name", name);
        spot.put("feature", feature);
        spot.put("bestTime", bestTime);
        spot.put("rating", rating);
        return spot;
    }

    private Map<String, Object> generatePeakDay() {
        Map<String, Object> peak = new HashMap<>();
        peak.put("day", "周六");
        peak.put("visitorCount", 8000 + new Random().nextInt(4000));
        return peak;
    }

    private Map<String, Object> generateQuietestDay() {
        Map<String, Object> quiet = new HashMap<>();
        quiet.put("day", "周二");
        quiet.put("visitorCount", 2000 + new Random().nextInt(2000));
        return quiet;
    }

    private Map<String, Integer> generateWeeklyPattern() {
        Map<String, Integer> pattern = new HashMap<>();
        pattern.put("monday", 3000 + new Random().nextInt(2000));
        pattern.put("tuesday", 2500 + new Random().nextInt(1500));
        pattern.put("wednesday", 2800 + new Random().nextInt(1500));
        pattern.put("thursday", 3200 + new Random().nextInt(1500));
        pattern.put("friday", 4500 + new Random().nextInt(2000));
        pattern.put("saturday", 7000 + new Random().nextInt(3000));
        pattern.put("sunday", 6000 + new Random().nextInt(2500));
        return pattern;
    }

    private List<Map<String, Object>> generateMonthlyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            Map<String, Object> data = new HashMap<>();
            data.put("month", month);
            data.put("visitors", 3000 + new Random().nextInt(7000));
            trend.add(data);
        }

        return trend;
    }
}
