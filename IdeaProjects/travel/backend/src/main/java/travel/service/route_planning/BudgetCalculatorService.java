package travel.service.route_planning;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预算计算器服务
 * 自动统计门票/交通/住宿总费用，支持明细调整
 */
public interface BudgetCalculatorService {

    /**
     * 计算路线总预算
     * @param routeId 路线ID
     * @param travelerCount 出行人数
     * @param days 天数
     * @return 预算明细
     */
    Map<String, Object> calculateTotalBudget(Integer routeId, Integer travelerCount, Integer days);

    /**
     * 获取费用明细分类
     * @param routeId 路线ID
     * @param travelerCount 出行人数
     * @return 分类费用明细
     */
    Map<String, BigDecimal> getExpenseBreakdown(Integer routeId, Integer travelerCount);

    /**
     * 计算门票费用
     * @param routeId 路线ID
     * @param travelerCount 人数
     * @return 门票费用
     */
    BigDecimal calculateTicketCost(Integer routeId, Integer travelerCount);

    /**
     * 计算交通费用
     * @param routeId 路线ID
     * @param travelerCount 人数
     * @param transportType 交通类型
     * @return 交通费用
     */
    BigDecimal calculateTransportCost(Integer routeId, Integer travelerCount, String transportType);

    /**
     * 计算住宿费用
     * @param cityId 城市ID
     * @param days 天数
     * @param travelerCount 人数
     * @param hotelLevel 酒店档次（economy-经济, standard-标准, luxury-豪华）
     * @return 住宿费用
     */
    BigDecimal calculateAccommodationCost(Integer cityId, Integer days, Integer travelerCount, String hotelLevel);

    /**
     * 计算餐饮费用
     * @param days 天数
     * @param travelerCount 人数
     * @param mealLevel 餐饮档次
     * @return 餐饮费用
     */
    BigDecimal calculateMealCost(Integer days, Integer travelerCount, String mealLevel);

    /**
     * 调整预算项目
     * @param routeId 路线ID
     * @param adjustments 调整项
     * @return 调整后的预算
     */
    Map<String, Object> adjustBudgetItems(Integer routeId, Map<String, BigDecimal> adjustments);

    /**
     * 获取预算优化建议
     * @param routeId 路线ID
     * @param budgetLimit 预算上限
     * @return 优化建议
     */
    List<Map<String, Object>> getBudgetOptimizationSuggestions(Integer routeId, BigDecimal budgetLimit);

    /**
     * 对比不同预算方案
     * @param routeId 路线ID
     * @param travelerCount 人数
     * @param days 天数
     * @return 方案对比
     */
    List<Map<String, Object>> compareBudgetPlans(Integer routeId, Integer travelerCount, Integer days);

    /**
     * 保存预算方案
     * @param routeId 路线ID
     * @param budgetPlan 预算方案
     * @return 保存结果
     */
    boolean saveBudgetPlan(Integer routeId, Map<String, Object> budgetPlan);

    /**
     * 获取人均费用分析
     * @param routeId 路线ID
     * @param travelerCount 人数
     * @return 人均费用
     */
    Map<String, Object> getPerCapitaCostAnalysis(Integer routeId, Integer travelerCount);

    /**
     * 导出费用清单
     * @param routeId 路线ID
     * @param format 格式（pdf, excel, json）
     * @return 导出结果
     */
    Map<String, Object> exportExpenseList(Integer routeId, String format);
}
