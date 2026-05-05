package travel.service.route_planning;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 旅行数据分析服务
 * 提供数据统计、趋势分析、用户行为分析等功能
 */
public interface TravelAnalyticsService {

    /**
     * 获取平台整体数据概览
     * @return 数据概览
     */
    Map<String, Object> getPlatformOverview();

    /**
     * 获取用户增长趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param groupBy 分组方式 (day/week/month)
     * @return 增长趋势数据
     */
    List<Map<String, Object>> getUserGrowthTrend(LocalDate startDate, LocalDate endDate, String groupBy);

    /**
     * 获取路线热度排行
     * @param limit 数量限制
     * @param timeRange 时间范围 (week/month/year)
     * @return 热度排行
     */
    List<Map<String, Object>> getRoutePopularityRanking(Integer limit, String timeRange);

    /**
     * 获取景点访问量统计
     * @param cityId 城市ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 访问量统计
     */
    List<Map<String, Object>> getAttractionVisitStats(Integer cityId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取用户行为分析
     * @param userId 用户ID
     * @return 行为分析数据
     */
    Map<String, Object> getUserBehaviorAnalysis(Integer userId);

    /**
     * 获取路线完成率统计
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 完成率统计
     */
    Map<String, Object> getRouteCompletionStats(LocalDate startDate, LocalDate endDate);

    /**
     * 获取预算分析
     * @param routeId 路线ID
     * @return 预算分析数据
     */
    Map<String, Object> getBudgetAnalysis(Integer routeId);

    /**
     * 获取交通方式偏好统计
     * @param cityId 城市ID
     * @return 交通方式偏好
     */
    Map<String, Object> getTransportPreferenceStats(Integer cityId);

    /**
     * 获取季节性趋势分析
     * @param cityId 城市ID
     * @param year 年份
     * @return 季节性趋势
     */
    List<Map<String, Object>> getSeasonalTrends(Integer cityId, Integer year);

    /**
     * 获取用户满意度分析
     * @param routeId 路线ID
     * @return 满意度分析
     */
    Map<String, Object> getSatisfactionAnalysis(Integer routeId);

    /**
     * 生成数据报表
     * @param reportType 报表类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param format 导出格式
     * @return 报表数据
     */
    Map<String, Object> generateReport(String reportType, LocalDate startDate, LocalDate endDate, String format);

    /**
     * 获取实时数据监控
     * @return 实时数据
     */
    Map<String, Object> getRealtimeMonitoring();

    /**
     * 获取热门搜索关键词
     * @param limit 数量限制
     * @param timeRange 时间范围
     * @return 热门关键词
     */
    List<Map<String, Object>> getHotSearchKeywords(Integer limit, String timeRange);

    /**
     * 获取用户留存率分析
     * @param cohortDate  cohort日期
     * @return 留存率数据
     */
    Map<String, Object> getUserRetentionAnalysis(LocalDate cohortDate);

    /**
     * 获取转化率漏斗分析
     * @return 转化率数据
     */
    Map<String, Object> getConversionFunnel();

    /**
     * 获取地理分布统计
     * @return 地理分布数据
     */
    List<Map<String, Object>> getGeographicDistribution();

    /**
     * 预测未来趋势
     * @param metric 指标类型
     * @param days 预测天数
     * @return 预测结果
     */
    Map<String, Object> predictFutureTrends(String metric, Integer days);

    /**
     * 获取异常检测报告
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 异常报告
     */
    List<Map<String, Object>> getAnomalyDetectionReport(LocalDate startDate, LocalDate endDate);

    /**
     * 对比分析
     * @param metric 指标
     * @param period1 时间段1
     * @param period2 时间段2
     * @return 对比结果
     */
    Map<String, Object> comparePeriods(String metric, Map<String, LocalDate> period1, Map<String, LocalDate> period2);

    /**
     * 获取用户画像分析
     * @return 用户画像数据
     */
    Map<String, Object> getUserPersonaAnalysis();

    /**
     * 获取推荐效果分析
     * @return 推荐效果数据
     */
    Map<String, Object> getRecommendationEffectAnalysis();
}
