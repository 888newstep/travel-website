package travel.service.route_planning;

import travel.entity.vo.RouteStatisticsVO;
import travel.entity.vo.TopRouteVO;

import java.util.List;
import java.util.Map;

/**
 * 路线统计分析服务接口
 */
public interface RouteStatisticsService {

    /**
     * 获取路线基础统计
     * @param routeId 路线ID
     * @return 基础统计信息
     */
    Map<String, Object> getBasicStatistics(Integer routeId);

    /**
     * 获取路线使用统计
     * @param routeId 路线ID
     * @param timeRange 时间范围
     * @return 使用统计信息
     */
    Map<String, Object> getUsageStatistics(Integer routeId, String timeRange);

    /**
     * 获取路线评分统计
     * @param routeId 路线ID
     * @return 评分统计信息
     */
    Map<String, Object> getRatingStatistics(Integer routeId);

    /**
     * 获取路线趋势分析
     * @param routeId 路线ID
     * @param days 天数
     * @return 趋势分析结果
     */
    Map<String, Object> getTrendAnalysis(Integer routeId, int days);

    /**
     * 获取用户偏好统计
     * @param userId 用户ID
     * @return 用户偏好统计
     */
    Map<String, Object> getUserPreferenceStatistics(Integer userId);

    /**
     * 获取热门路线
     * @param cityId 城市ID
     * @param limit 限制数量
     * @return 热门路线列表
     */
    List<Map<String, Object>> getPopularRoutes(String cityId, int limit);

    /**
     * 生成统计报告
     * @param reportConfig 报告配置
     * @return 报告URL
     */
    String generateStatisticsReport(Map<String, Object> reportConfig);

    /**
     * 比较路线统计
     * @param routeIds 路线ID列表
     * @return 对比结果
     */
    Map<String, Object> compareRouteStatistics(List<Integer> routeIds);

    /**
     * 获取路线完成率统计
     * @param routeId 路线ID
     * @return 完成率统计
     */
    Map<String, Object> getCompletionRateStatistics(Integer routeId);

    /**
     * 导出统计数据
     * @param exportConfig 导出配置
     * @return 下载URL
     */
    String exportStatisticsData(Map<String, Object> exportConfig);

    // 以下是实现类中额外的方法

    /**
     * 获取路线统计VO
     * @param routeId 路线ID
     * @return 路线统计VO
     */
    RouteStatisticsVO getRouteStatistics(Long routeId);

    /**
     * 获取路线访问趋势
     * @param routeId 路线ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 访问趋势
     */
    Map<java.time.LocalDate, Integer> getRouteAccessTrend(Long routeId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取Top路线VO列表
     * @param limit 限制数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Top路线VO列表
     */
    List<TopRouteVO> getTopRoutes(int limit, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 获取路线列表统计
     * @param routeIds 路线ID列表
     * @return 统计列表
     */
    List<Map<String, Object>> getRouteListStatistics(List<Long> routeIds);

    /**
     * 获取用户路线统计
     * @param userId 用户ID
     * @return 用户路线统计
     */
    Map<String, Object> getUserRouteStatistics(Integer userId);

    /**
     * 获取路线收藏统计
     * @param routeId 路线ID
     * @return 收藏统计
     */
    Map<String, Object> getRouteCollectionStatistics(Integer routeId);

    /**
     * 获取路线访问趋势
     * @param routeId 路线ID
     * @param days 天数
     * @return 访问趋势
     */
    Map<String, Object> getRouteAccessTrend(Integer routeId, Integer days);

    /**
     * 获取路线排名
     * @param type 类型
     * @param limit 限制数量
     * @return 路线排名
     */
    List<Map<String, Object>> getRouteRanking(String type, Integer limit);

    /**
     * 获取城市路线统计
     * @param cityId 城市ID
     * @return 城市路线统计
     */
    Map<String, Object> getCityRouteStatistics(String cityId);

    /**
     * 获取Top路线VO列表
     * @param type 类型
     * @param cityId 城市ID
     * @param limit 限制数量
     * @return Top路线VO列表
     */
    List<Map<String, Object>> getTopRoutes(String type, String cityId, Integer limit);

    // 以下是实现类中额外的方法

    /**
     * 获取用户路线统计
     * @param userId 用户ID
     * @return 用户路线统计
     */
    Map<String, Object> getUserRouteStatistics(Long userId);

    /**
     * 获取路线类型分布
     * @return 路线类型分布
     */
    Map<String, Integer> getRouteTypeDistribution();

    /**
     * 获取路线统计列表
     * @param page 页码
     * @param size 每页数量
     * @param orderBy 排序字段
     * @param orderDir 排序方向
     * @return 路线统计列表
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<RouteStatisticsVO> getRouteStatisticsList(int page, int size, String orderBy, String orderDir);

    /**
     * 获取路线收藏趋势
     * @param routeId 路线ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收藏趋势
     */
    Map<java.time.LocalDate, Integer> getRouteCollectionTrend(Long routeId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取路线评论统计
     * @param routeId 路线ID
     * @return 评论统计
     */
    Map<String, Object> getRouteCommentStatistics(Long routeId);
}
