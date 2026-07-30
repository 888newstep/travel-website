package travel.route.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import travel.common.entity.route_planning.Route;
import travel.common.entity.user_community.RouteComment;
import travel.common.entity.user_community.RouteCollection;
import travel.common.entity.user_community.RouteShare;
import travel.common.vo.RouteStatisticsVO;
import travel.common.vo.TopRouteVO;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.route_planning_mapper.RouteCollectionMapper;
import travel.common.mapper.route_planning_mapper.RouteShareMapper;
import travel.common.mapper.route_planning_mapper.RouteCommentMapper;
import travel.route.service.RouteStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 路线统计分析服务实现
 */
@Service
public class RouteStatisticsServiceImpl implements RouteStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(RouteStatisticsServiceImpl.class);

    private final RouteMapper routeMapper;
    private final RouteShareMapper routeShareMapper;
    private final RouteCommentMapper routeCommentMapper;
    private final RouteCollectionMapper routeCollectionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public RouteStatisticsServiceImpl(RouteMapper routeMapper, RouteShareMapper routeShareMapper,
                                       RouteCommentMapper routeCommentMapper, RouteCollectionMapper routeCollectionMapper,
                                       RedisTemplate<String, Object> redisTemplate) {
        this.routeMapper = routeMapper;
        this.routeShareMapper = routeShareMapper;
        this.routeCommentMapper = routeCommentMapper;
        this.routeCollectionMapper = routeCollectionMapper;
        this.redisTemplate = redisTemplate;
    }

    // 缓存键前缀
    private static final String ROUTE_STATISTICS_PREFIX = "route:statistics:";
    private static final String ROUTE_ACCESS_TREND_PREFIX = "route:access:trend:";
    private static final String TOP_ROUTES_PREFIX = "route:top:";
    private static final String USER_ROUTE_STATISTICS_PREFIX = "user:route:statistics:";
    private static final String ROUTE_TYPE_DISTRIBUTION_PREFIX = "route:type:distribution";
    private static final String ROUTE_COLLECTION_TREND_PREFIX = "route:collection:trend:";
    private static final String ROUTE_COMMENT_STATISTICS_PREFIX = "route:comment:statistics:";

    // 缓存过期时间（小时）
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Override
    public RouteStatisticsVO getRouteStatistics(Long routeId) {
        // 尝试从缓存获取
        String cacheKey = ROUTE_STATISTICS_PREFIX + routeId;
        RouteStatisticsVO statisticsVO = (RouteStatisticsVO) redisTemplate.opsForValue().get(cacheKey);
        if (statisticsVO != null) {
            return statisticsVO;
        }

        // 从数据库查询
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        // 构建统计信息
        statisticsVO = new RouteStatisticsVO();
        statisticsVO.setRouteId(route.getId().longValue()); // 将Integer转换为Long
        statisticsVO.setRouteName(route.getTitle()); // 使用title字段
        statisticsVO.setRouteType(route.getDifficulty()); // 使用difficulty字段
        statisticsVO.setRouteLength(0.0); // 暂时设置为0.0
        statisticsVO.setEstimatedTime(0.0); // 暂时设置为0.0
        statisticsVO.setEstimatedCost(0.0); // 暂时设置为0.0
        statisticsVO.setCreateTime(route.getCreatedAt().toString());

        // 获取访问次数
        statisticsVO.setVisitCount(route.getViewCount() != null ? route.getViewCount() : 0);

        // 获取收藏次数
        int collectionCount = routeCollectionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteCollection>()
                .eq("item_id", routeId).eq("item_type", "route")).size();
        statisticsVO.setCollectionCount(collectionCount);

        // 获取分享次数
        int shareCount = routeShareMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteShare>().eq("route_id", routeId)).size();
        statisticsVO.setShareCount(shareCount);

        // 获取评价次数和平均评分
        List<RouteComment> comments = routeCommentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteComment>().eq("route_id", routeId));
        statisticsVO.setCommentCount(comments.size());
        if (!comments.isEmpty()) {
            double averageScore = comments.stream()
                    .mapToDouble(RouteComment::getRating)
                    .average()
                    .orElse(0.0);
            statisticsVO.setAverageScore(averageScore);
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, statisticsVO, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return statisticsVO;
    }

    @Override
    public Map<LocalDate, Integer> getRouteAccessTrend(Long routeId, LocalDate startDate, LocalDate endDate) {
        // 尝试从缓存获取
        String cacheKey = ROUTE_ACCESS_TREND_PREFIX + routeId + ":" + startDate + ":" + endDate;
        Object trendDataObj = redisTemplate.opsForValue().get(cacheKey);
        Map<LocalDate, Integer> trendData = null;
        if (trendDataObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) trendDataObj;
            trendData = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof LocalDate && entry.getValue() instanceof Integer) {
                    trendData.put((LocalDate) entry.getKey(), (Integer) entry.getValue());
                }
            }
        }
        if (trendData != null) {
            return trendData;
        }

        // 从数据库查询（这里需要根据实际的访问记录存储方式实现）
        // 假设我们有一个访问记录��，或者需要从其他表统计
        trendData = new TreeMap<>();

        // 初始化日期范围
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            trendData.put(currentDate, 0);
            currentDate = currentDate.plusDays(1);
        }

        // 这里需要根据实际的访问记录存储方式实现查询逻辑
        // 例如：List<Map<String, Object>> accessRecords = routeMapper.selectAccessRecordsByDateRange(routeId, startDate, endDate);
        // 然后遍历访问记录，更新trendData

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, trendData, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return trendData;
    }

    @Override
    public List<TopRouteVO> getTopRoutes(int limit, LocalDateTime startTime, LocalDateTime endTime) {
        // 尝试从缓存获取
        String cacheKey = TOP_ROUTES_PREFIX + limit + ":" + startTime + ":" + endTime;
        Object topRoutesObj = redisTemplate.opsForValue().get(cacheKey);
        List<TopRouteVO> topRoutes = new ArrayList<>();
        if (topRoutesObj instanceof List) {
            List<?> list = (List<?>) topRoutesObj;
            for (Object item : list) {
                if (item instanceof TopRouteVO) {
                    topRoutes.add((TopRouteVO) item);
                }
            }
        }
        if (!topRoutes.isEmpty()) {
            return topRoutes;
        }

        // 从数据库查询
        List<Route> routes = routeMapper.selectList(null);

        // 计算每个路线的综合得分
        List<TopRouteVO> routeVOs = new ArrayList<>();
        for (Route route : routes) {
            TopRouteVO vo = new TopRouteVO();
            vo.setRouteId(route.getId().longValue()); // 将Integer转换为Long
            vo.setRouteName(route.getTitle()); // 使用title字段
            vo.setRouteType(route.getDifficulty()); // 使用difficulty字段
            vo.setVisitCount(route.getViewCount() != null ? route.getViewCount() : 0); // 使用viewCount字段

            // 获取收藏次数
            int collectionCount = routeCollectionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteCollection>()
                    .eq("item_id", route.getId()).eq("item_type", "route")).size();
            vo.setCollectionCount(collectionCount);

            // 获取分享次数
            int shareCount = routeShareMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteShare>().eq("route_id", route.getId())).size();
            vo.setShareCount(shareCount);

            // 获取评价次数和平均评分
            List<RouteComment> comments = routeCommentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteComment>().eq("route_id", route.getId()));
            vo.setCommentCount(comments.size());
            if (!comments.isEmpty()) {
                double averageScore = comments.stream()
                        .mapToDouble(RouteComment::getRating)
                        .average()
                        .orElse(0.0);
                vo.setAverageScore(averageScore);
            }

            routeVOs.add(vo);
        }

        // 排序并限制数量
        routeVOs.sort((a, b) -> {
            // 综合得分计算：访问次数 * 0.3 + 收藏次数 * 0.3 + 分享次数 * 0.2 + 评价次数 * 0.1 + 平均评分 * 5 * 0.1
            double scoreA = a.getVisitCount() * 0.3 + a.getCollectionCount() * 0.3 + a.getShareCount() * 0.2 + a.getCommentCount() * 0.1 + (a.getAverageScore() != null ? a.getAverageScore() : 0) * 5 * 0.1;
            double scoreB = b.getVisitCount() * 0.3 + b.getCollectionCount() * 0.3 + b.getShareCount() * 0.2 + b.getCommentCount() * 0.1 + (b.getAverageScore() != null ? b.getAverageScore() : 0) * 5 * 0.1;
            return Double.compare(scoreB, scoreA);
        });

        if (routeVOs.size() > limit) {
            routeVOs = routeVOs.subList(0, limit);
        }

        // 设置排名
        for (int i = 0; i < routeVOs.size(); i++) {
            routeVOs.get(i).setRank(i + 1);
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, routeVOs, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return routeVOs;
    }

    @Override
    public List<Map<String, Object>> getTopRoutes(String type, String cityId, Integer limit) {
        log.info("获取Top路线: type={}, cityId={}, limit={}", type, cityId, limit);
        List<Map<String, Object>> topRoutes = new ArrayList<>();

        // 从数据库查询路线
        LambdaQueryWrapper<Route> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Route::getDifficulty, type);
        }
        if (cityId != null && !cityId.isEmpty()) {
            queryWrapper.eq(Route::getCityId, cityId);
        }
        queryWrapper.orderByDesc(Route::getViewCount);
        queryWrapper.last("LIMIT " + limit);

        List<Route> routes = routeMapper.selectList(queryWrapper);

        // 转换为Map列表
        for (Route route : routes) {
            Map<String, Object> routeMap = new HashMap<>();
            routeMap.put("routeId", route.getId());
            routeMap.put("routeName", route.getTitle());
            routeMap.put("routeType", route.getDifficulty());
            routeMap.put("visitCount", route.getViewCount());
            routeMap.put("createTime", route.getCreatedAt());
            topRoutes.add(routeMap);
        }

        return topRoutes;
    }

    @Override
    public Map<String, Object> getCityRouteStatistics(String cityId) {
        log.info("获取城市路线统计: cityId={}", cityId);
        Map<String, Object> statistics = new HashMap<>();

        // 获取该城市的路线总数
        LambdaQueryWrapper<Route> routeWrapper = new LambdaQueryWrapper<>();
        routeWrapper.eq(Route::getCityId, cityId);
        long routeCount = routeMapper.selectCount(routeWrapper);
        statistics.put("routeCount", (int) routeCount);

        // 获取该城市的路线浏览量统计
        List<Route> routes = routeMapper.selectList(routeWrapper);
        int totalViews = routes.stream().mapToInt(r -> r.getViewCount() != null ? r.getViewCount() : 0).sum();
        statistics.put("totalViews", totalViews);

        // 获取收藏数量
        List<Integer> routeIds = routes.stream().map(Route::getId).collect(Collectors.toList());
        long collectionCount = 0;
        if (!routeIds.isEmpty()) {
            LambdaQueryWrapper<RouteCollection> collectionWrapper = new LambdaQueryWrapper<>();
            collectionWrapper.in(RouteCollection::getRouteId, routeIds.toArray(new Object[0]))
                    .eq(RouteCollection::getItemType, "route");
            collectionCount = routeCollectionMapper.selectCount(collectionWrapper);
        }
        statistics.put("collectionCount", (int) collectionCount);

        // 获取评论数量
        long commentCount = 0;
        if (!routeIds.isEmpty()) {
            LambdaQueryWrapper<RouteComment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.in(RouteComment::getRouteId, routeIds.toArray(new Object[0]));
            commentCount = routeCommentMapper.selectCount(commentWrapper);
        }
        statistics.put("commentCount", (int) commentCount);

        return statistics;
    }

    @Override
    public List<Map<String, Object>> getRouteRanking(String type, Integer limit) {
        log.info("获取路线排名: type={}, limit={}", type, limit);
        List<Map<String, Object>> ranking = new ArrayList<>();

        LambdaQueryWrapper<Route> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Route::getDifficulty, type);
        }
        queryWrapper.orderByDesc(Route::getViewCount);
        queryWrapper.last("LIMIT " + limit);

        List<Route> routes = routeMapper.selectList(queryWrapper);

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("routeId", route.getId());
            item.put("routeName", route.getTitle());
            item.put("viewCount", route.getViewCount());
            item.put("cityId", route.getCityId());
            item.put("difficulty", route.getDifficulty());
            ranking.add(item);
        }

        return ranking;
    }

    @Override
    public Map<String, Object> getRouteAccessTrend(Integer routeId, Integer days) {
        log.info("获取路线访问趋势: routeId={}, days={}", routeId, days);
        Map<String, Object> trendData = new HashMap<>();

        // 计算日期范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 从缓存或数据库获取访问数据
        String cacheKey = ROUTE_ACCESS_TREND_PREFIX + routeId + ":" + startDate + ":" + endDate;
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);

        if (cachedData instanceof Map) {
            Map<?, ?> cachedMap = (Map<?, ?>) cachedData;
            for (Map.Entry<?, ?> entry : cachedMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    trendData.put((String) entry.getKey(), entry.getValue());
                }
            }
            return trendData;
        }

        // 生成模拟数据（实际项目中应从数据库查询）
        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", (int) (Math.random() * 100));
            dailyData.add(dayData);
        }

        trendData.put("routeId", routeId);
        trendData.put("startDate", startDate.toString());
        trendData.put("endDate", endDate.toString());
        trendData.put("dailyData", dailyData);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, trendData, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return trendData;
    }

    @Override
    public List<Map<String, Object>> getRouteListStatistics(List<Long> routeIds) {
        log.info("获取路线列表统计: routeIds={}", routeIds);
        List<Map<String, Object>> statisticsList = new ArrayList<>();

        if (routeIds == null || routeIds.isEmpty()) {
            return statisticsList;
        }

        for (Long routeId : routeIds) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("routeId", routeId);

            // 获取路线基本信息
            Route route = routeMapper.selectById(routeId.intValue());
            if (route != null) {
                stats.put("routeName", route.getTitle());
                stats.put("viewCount", route.getViewCount());
                stats.put("difficulty", route.getDifficulty());
            }

            // 获取收藏数
            LambdaQueryWrapper<RouteCollection> collectionWrapper = new LambdaQueryWrapper<>();
            collectionWrapper.eq(RouteCollection::getRouteId, routeId.intValue())
                    .eq(RouteCollection::getItemType, "route");
            long collectionCount = routeCollectionMapper.selectCount(collectionWrapper);
            stats.put("collectionCount", (int) collectionCount);

            // 获取评论数
            LambdaQueryWrapper<RouteComment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.eq(RouteComment::getRouteId, routeId.intValue());
            long commentCount = routeCommentMapper.selectCount(commentWrapper);
            stats.put("commentCount", (int) commentCount);

            statisticsList.add(stats);
        }

        return statisticsList;
    }

    @Override
    public Map<String, Object> getRouteCollectionStatistics(Integer routeId) {
        log.info("获取路线收藏统计: routeId={}", routeId);
        Map<String, Object> statistics = new HashMap<>();

        // 获取总收藏数
        LambdaQueryWrapper<RouteCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getItemType, "route");
        long totalCount = routeCollectionMapper.selectCount(wrapper);
        statistics.put("totalCount", (int) totalCount);

        // 获取今日收藏数
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<RouteCollection> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getItemType, "route")
                .ge(RouteCollection::getCollectionTime, startOfDay);
        long todayCount = routeCollectionMapper.selectCount(todayWrapper);
        statistics.put("todayCount", (int) todayCount);

        // 获取本周收藏数
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LambdaQueryWrapper<RouteCollection> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getItemType, "route")
                .ge(RouteCollection::getCollectionTime, startOfWeek);
        long weekCount = routeCollectionMapper.selectCount(weekWrapper);
        statistics.put("weekCount", (int) weekCount);

        return statistics;
    }

    @Override
    public Map<String, Object> getUserRouteStatistics(Integer userId) {
        // 将Integer转换为Long并调用Long版本
        return getUserRouteStatistics(userId.longValue());
    }

    @Override
    public Map<String, Object> getUserRouteStatistics(Long userId) {
        // 尝试从缓存获取
        String cacheKey = USER_ROUTE_STATISTICS_PREFIX + userId;
        Object statisticsObj = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Object> statistics = null;
        if (statisticsObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) statisticsObj;
            statistics = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    statistics.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        if (statistics != null) {
            return statistics;
        }

        // 从数据库查询
        statistics = new HashMap<>();

        // 获取用户创建的路线数量
        int createdRouteCount = routeMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Route>().eq("user_id", userId)).size();
        statistics.put("createdRouteCount", createdRouteCount);

        // 获取用户收藏的路线数量
        int collectionCount = routeCollectionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteCollection>()
                .eq("user_id", userId).eq("item_type", "route")).size();
        statistics.put("collectionCount", collectionCount);

        // 获取用户分享的路线数量
        int shareCount = routeShareMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteShare>().eq("user_id", userId)).size();
        statistics.put("shareCount", shareCount);

        // 获取用户评价的路线数量
        int commentCount = routeCommentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteComment>().eq("user_id", userId)).size();
        statistics.put("commentCount", commentCount);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, statistics, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return statistics;
    }

    @Override
    public Map<String, Integer> getRouteTypeDistribution() {
        // 尝试从缓存获取
        String cacheKey = ROUTE_TYPE_DISTRIBUTION_PREFIX;
        Object distributionObj = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Integer> distribution = null;
        if (distributionObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) distributionObj;
            distribution = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Integer) {
                    distribution.put((String) entry.getKey(), (Integer) entry.getValue());
                }
            }
        }
        if (distribution != null) {
            return distribution;
        }

        // 从数据库查询
        List<Route> routes = routeMapper.selectList(null);
        distribution = routes.stream()
                .collect(Collectors.groupingBy(Route::getDifficulty, Collectors.summingInt(r -> 1))); // 使用difficulty字段

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, distribution, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return distribution;
    }

    @Override
    public Page<RouteStatisticsVO> getRouteStatisticsList(int page, int size, String orderBy, String orderDir) {
        // 创建分页对象
        Page<Route> routePage = new Page<>(page, size);

        // 构建查询条件
        // 这里需要根据实际的排序字段实现查询逻辑
        // 例如：QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        // if ("visitCount".equals(orderBy)) {
        //     queryWrapper.orderByDesc(orderDir.equals("desc"), "visit_count");
        // }
        // routeMapper.selectPage(routePage, queryWrapper);

        // 模拟查询结果
        routePage.setRecords(Collections.emptyList());
        routePage.setTotal(0);

        // 转换为VO
        Page<RouteStatisticsVO> statisticsPage = new Page<>(page, size);
        statisticsPage.setTotal(routePage.getTotal());
        statisticsPage.setRecords(routePage.getRecords().stream()
                .map(route -> {
                    RouteStatisticsVO vo = new RouteStatisticsVO();
                    vo.setRouteId(route.getId().longValue()); // 将Integer转换为Long
                    vo.setRouteName(route.getTitle()); // 使用title字段
                    vo.setRouteType(route.getDifficulty()); // 使用difficulty字段
                    vo.setVisitCount(route.getViewCount() != null ? route.getViewCount() : 0); // 使用viewCount字段
                    return vo;
                })
                .collect(Collectors.toList()));

        return statisticsPage;
    }

    @Override
    public Map<LocalDate, Integer> getRouteCollectionTrend(Long routeId, LocalDate startDate, LocalDate endDate) {
        // 尝试从缓存获取
        String cacheKey = ROUTE_COLLECTION_TREND_PREFIX + routeId + ":" + startDate + ":" + endDate;
        Object trendDataObj = redisTemplate.opsForValue().get(cacheKey);
        Map<LocalDate, Integer> trendData = null;
        if (trendDataObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) trendDataObj;
            trendData = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof LocalDate && entry.getValue() instanceof Integer) {
                    trendData.put((LocalDate) entry.getKey(), (Integer) entry.getValue());
                }
            }
        }
        if (trendData != null) {
            return trendData;
        }

        // 从数据库查询
        trendData = new TreeMap<>();

        // 初始化日期范围
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            trendData.put(currentDate, 0);
            currentDate = currentDate.plusDays(1);
        }

        // 这里需要根据实际的收藏记录存储方式实现查询逻辑
        // 例如：List<Map<String, Object>> collectionRecords = routeCollectionMapper.selectCollectionRecordsByDateRange(routeId, startDate, endDate);
        // 然后遍历收藏记录，更新trendData

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, trendData, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return trendData;
    }

    @Override
    public Map<String, Object> getRouteCommentStatistics(Long routeId) {
        // 尝试从缓存获取
        String cacheKey = ROUTE_COMMENT_STATISTICS_PREFIX + routeId;
        Object statisticsObj = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Object> statistics = null;
        if (statisticsObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) statisticsObj;
            statistics = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    statistics.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        if (statistics != null) {
            return statistics;
        }

        // 从数据库查询
        statistics = new HashMap<>();

        // 获取评价总数
        List<RouteComment> comments = routeCommentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RouteComment>().eq("route_id", routeId));
        statistics.put("totalCommentCount", comments.size());

        // 计算平均评分
        if (!comments.isEmpty()) {
            double averageScore = comments.stream()
                    .mapToDouble(RouteComment::getRating)
                    .average()
                    .orElse(0.0);
            statistics.put("averageScore", averageScore);

            // 计算各评分等级的数量
            Map<Double, Long> scoreDistribution = comments.stream()
                    .collect(Collectors.groupingBy(RouteComment::getRating, Collectors.counting()));
            statistics.put("scoreDistribution", scoreDistribution);
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, statistics, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return statistics;
    }

    @Override
    public Map<String, Object> getBasicStatistics(Integer routeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("views", 1234);
        result.put("likes", 567);
        result.put("comments", 89);
        return result;
    }

    @Override
    public Map<String, Object> getUsageStatistics(Integer routeId, String timeRange) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("timeRange", timeRange);
        result.put("usageCount", 987);
        return result;
    }

    @Override
    public Map<String, Object> getRatingStatistics(Integer routeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("averageRating", 4.5);
        result.put("totalRatings", 123);
        return result;
    }

    @Override
    public Map<String, Object> getTrendAnalysis(Integer routeId, int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("days", days);
        result.put("trend", List.of(100, 120, 150, 130, 140, 160, 180));
        return result;
    }

    @Override
    public Map<String, Object> getUserPreferenceStatistics(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("preferredTypes", List.of("自然风光", "历史文化"));
        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularRoutes(String cityId, int limit) {
        List<Map<String, Object>> routes = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Map<String, Object> route = new HashMap<>();
            route.put("id", i + 1);
            route.put("name", "热门路线 " + (i + 1));
            route.put("views", 1000 + i * 100);
            routes.add(route);
        }
        return routes;
    }

    @Override
    public String generateStatisticsReport(Map<String, Object> reportConfig) {
        return "http://example.com/reports/report_" + System.currentTimeMillis() + ".pdf";
    }

    @Override
    public Map<String, Object> compareRouteStatistics(List<Integer> routeIds) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeIds", routeIds);
        result.put("comparison", Map.of("views", List.of(1234, 5678), "likes", List.of(567, 890)));
        return result;
    }

    @Override
    public Map<String, Object> getCompletionRateStatistics(Integer routeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("completionRate", 0.85);
        return result;
    }

    @Override
    public String exportStatisticsData(Map<String, Object> exportConfig) {
        return "http://example.com/exports/export_" + System.currentTimeMillis() + ".csv";
    }
}
