package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.BudgetCalculatorService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BudgetCalculatorServiceImpl implements BudgetCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(BudgetCalculatorServiceImpl.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String BUDGET_PREFIX = "budget:";

    // 费用标准配置 - 使用ConcurrentHashMap保证线程安全
    private static final Map<String, BigDecimal> HOTEL_PRICES = new ConcurrentHashMap<>();
    private static final Map<String, BigDecimal> MEAL_PRICES = new ConcurrentHashMap<>();
    private static final Map<String, BigDecimal> TRANSPORT_PRICES = new ConcurrentHashMap<>();

    static {
        // 酒店价格（每晚/间）
        HOTEL_PRICES.put("economy", new BigDecimal("200"));
        HOTEL_PRICES.put("standard", new BigDecimal("400"));
        HOTEL_PRICES.put("luxury", new BigDecimal("800"));

        // 餐饮价格（每人每天）
        MEAL_PRICES.put("economy", new BigDecimal("80"));
        MEAL_PRICES.put("standard", new BigDecimal("150"));
        MEAL_PRICES.put("luxury", new BigDecimal("300"));

        // 交通价格（每人每天）
        TRANSPORT_PRICES.put("public", new BigDecimal("50"));
        TRANSPORT_PRICES.put("taxi", new BigDecimal("150"));
        TRANSPORT_PRICES.put("car", new BigDecimal("200"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> calculateTotalBudget(Integer routeId, Integer travelerCount, Integer days) {
        String cacheKey = BUDGET_PREFIX + routeId + ":" + travelerCount + ":" + days;

        Map<String, Object> cachedBudget = cacheUtil.get(cacheKey, Map.class);
        if (cachedBudget != null) {
            return cachedBudget;
        }

        Route route = routeService.getById(routeId);
        if (route == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> budget = new HashMap<>();
        budget.put("routeId", routeId);
        budget.put("routeName", route.getTitle());
        budget.put("travelerCount", travelerCount);
        budget.put("days", days);

        // 计算各项费用
        BigDecimal ticketCost = calculateTicketCost(routeId, travelerCount);
        BigDecimal transportCost = calculateTransportCost(routeId, travelerCount, "public");
        BigDecimal accommodationCost = calculateAccommodationCost(
                route.getCity() != null ? route.getCity().getId() : null,
                days, travelerCount, "standard");
        BigDecimal mealCost = calculateMealCost(days, travelerCount, "standard");
        BigDecimal otherCost = calculateOtherCost(days, travelerCount);

        BigDecimal totalCost = ticketCost.add(transportCost).add(accommodationCost)
                .add(mealCost).add(otherCost);

        budget.put("ticketCost", ticketCost);
        budget.put("transportCost", transportCost);
        budget.put("accommodationCost", accommodationCost);
        budget.put("mealCost", mealCost);
        budget.put("otherCost", otherCost);
        budget.put("totalCost", totalCost);
        budget.put("perCapitaCost", totalCost.divide(new BigDecimal(travelerCount), 2, RoundingMode.HALF_UP));

        // 费用明细
        budget.put("breakdown", generateBudgetBreakdown(ticketCost, transportCost,
                accommodationCost, mealCost, otherCost, totalCost));

        cacheUtil.set(cacheKey, budget, 30, java.util.concurrent.TimeUnit.MINUTES);

        return budget;
    }

    @Override
    public Map<String, BigDecimal> getExpenseBreakdown(Integer routeId, Integer travelerCount) {
        Map<String, BigDecimal> breakdown = new HashMap<>();

        breakdown.put("tickets", calculateTicketCost(routeId, travelerCount));
        breakdown.put("transport", calculateTransportCost(routeId, travelerCount, "public"));
        breakdown.put("meals", BigDecimal.ZERO); // 需要days参数
        breakdown.put("shopping", new BigDecimal("200").multiply(new BigDecimal(travelerCount)));
        breakdown.put("entertainment", new BigDecimal("100").multiply(new BigDecimal(travelerCount)));
        breakdown.put("miscellaneous", new BigDecimal("50").multiply(new BigDecimal(travelerCount)));

        return breakdown;
    }

    @Override
    public BigDecimal calculateTicketCost(Integer routeId, Integer travelerCount) {
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        BigDecimal totalTicketCost = BigDecimal.ZERO;
        for (RouteAttraction ra : routeAttractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getTicketPrice() != null) {
                totalTicketCost = totalTicketCost.add(attraction.getTicketPrice());
            }
        }

        return totalTicketCost.multiply(new BigDecimal(travelerCount));
    }

    @Override
    public BigDecimal calculateTransportCost(Integer routeId, Integer travelerCount, String transportType) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyTransportCost = TRANSPORT_PRICES.getOrDefault(transportType, new BigDecimal("50"));
        return dailyTransportCost.multiply(new BigDecimal(travelerCount))
                .multiply(new BigDecimal(route.getDurationDays()));
    }

    @Override
    public BigDecimal calculateAccommodationCost(Integer cityId, Integer days, Integer travelerCount, String hotelLevel) {
        BigDecimal roomPrice = HOTEL_PRICES.getOrDefault(hotelLevel, new BigDecimal("400"));

        // 计算需要的房间数（假设每间房住2人）
        int roomCount = (int) Math.ceil(travelerCount / 2.0);

        // 住宿天数 = 总天数 - 1（最后一天不需要住宿）
        int accommodationDays = Math.max(0, days - 1);

        return roomPrice.multiply(new BigDecimal(roomCount))
                .multiply(new BigDecimal(accommodationDays));
    }

    @Override
    public BigDecimal calculateMealCost(Integer days, Integer travelerCount, String mealLevel) {
        BigDecimal dailyMealCost = MEAL_PRICES.getOrDefault(mealLevel, new BigDecimal("150"));
        return dailyMealCost.multiply(new BigDecimal(travelerCount))
                .multiply(new BigDecimal(days));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> adjustBudgetItems(Integer routeId, Map<String, BigDecimal> adjustments) {
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("adjustments", adjustments);
        result.put("adjustedItems", new ArrayList<Map<String, Object>>());

        BigDecimal totalAdjustment = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : adjustments.entrySet()) {
            totalAdjustment = totalAdjustment.add(entry.getValue());

            Map<String, Object> item = new HashMap<>();
            item.put("category", entry.getKey());
            item.put("adjustment", entry.getValue());
            item.put("type", entry.getValue().compareTo(BigDecimal.ZERO) > 0 ? "increase" : "decrease");

            ((List<Map<String, Object>>) result.get("adjustedItems")).add(item);
        }

        result.put("totalAdjustment", totalAdjustment);

        return result;
    }

    @Override
    public List<Map<String, Object>> getBudgetOptimizationSuggestions(Integer routeId, BigDecimal budgetLimit) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            return suggestions;
        }

        // 建议1：选择经济型住宿
        Map<String, Object> suggestion1 = new HashMap<>();
        suggestion1.put("type", "accommodation");
        suggestion1.put("title", "选择经济型住宿");
        suggestion1.put("description", "选择青旅或经济型酒店，可节省40%住宿费用");
        suggestion1.put("potentialSavings", new BigDecimal("200"));
        suggestion1.put("impact", "medium");
        suggestions.add(suggestion1);

        // 建议2：使用公共交通
        Map<String, Object> suggestion2 = new HashMap<>();
        suggestion2.put("type", "transport");
        suggestion2.put("title", "多使用公共交通");
        suggestion2.put("description", "以地铁公交代替出租车，可节省50%交通费用");
        suggestion2.put("potentialSavings", new BigDecimal("100"));
        suggestion2.put("impact", "low");
        suggestions.add(suggestion2);

        // 建议3：选择免费景点
        Map<String, Object> suggestion3 = new HashMap<>();
        suggestion3.put("type", "attractions");
        suggestion3.put("title", "增加免费景点");
        suggestion3.put("description", "适当增加公园、广场等免费景点");
        suggestion3.put("potentialSavings", new BigDecimal("150"));
        suggestion3.put("impact", "low");
        suggestions.add(suggestion3);

        // 建议4：错峰用餐
        Map<String, Object> suggestion4 = new HashMap<>();
        suggestion4.put("type", "meals");
        suggestion4.put("title", "选择当地小吃");
        suggestion4.put("description", "尝试当地特色小吃代替正餐，既省钱又体验当地文化");
        suggestion4.put("potentialSavings", new BigDecimal("80"));
        suggestion4.put("impact", "low");
        suggestions.add(suggestion4);

        return suggestions;
    }

    @Override
    public List<Map<String, Object>> compareBudgetPlans(Integer routeId, Integer travelerCount, Integer days) {
        List<Map<String, Object>> plans = new ArrayList<>();

        // 经济型方案
        plans.add(createBudgetPlan("economy", "经济型", routeId, travelerCount, days,
                "economy", "public", "economy"));

        // 标准型方案
        plans.add(createBudgetPlan("standard", "舒适型", routeId, travelerCount, days,
                "standard", "public", "standard"));

        // 豪华型方案
        plans.add(createBudgetPlan("luxury", "豪华型", routeId, travelerCount, days,
                "luxury", "taxi", "luxury"));

        return plans;
    }

    @Override
    public boolean saveBudgetPlan(Integer routeId, Map<String, Object> budgetPlan) {
        try {
            // 保存预算方案到数据库或缓存
            String cacheKey = BUDGET_PREFIX + "saved:" + routeId + ":" + System.currentTimeMillis();
            cacheUtil.set(cacheKey, budgetPlan, 7, java.util.concurrent.TimeUnit.DAYS);
            log.info("预算方案已保存: routeId={}", routeId);
            return true;
        } catch (Exception e) {
            log.error("保存预算方案失败: routeId={}, error={}", routeId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getPerCapitaCostAnalysis(Integer routeId, Integer travelerCount) {
        Map<String, Object> analysis = new HashMap<>();

        BigDecimal ticketCost = calculateTicketCost(routeId, 1); // 人均门票
        BigDecimal transportCost = calculateTransportCost(routeId, 1, "public"); // 人均交通

        analysis.put("routeId", routeId);
        analysis.put("travelerCount", travelerCount);
        analysis.put("perCapitaTicket", ticketCost);
        analysis.put("perCapitaTransport", transportCost);
        analysis.put("perCapitaMeal", new BigDecimal("150"));
        analysis.put("perCapitaAccommodation", new BigDecimal("200"));
        analysis.put("perCapitaTotal", ticketCost.add(transportCost)
                .add(new BigDecimal("150")).add(new BigDecimal("200")));

        // 费用占比分析
        analysis.put("costDistribution", generateCostDistribution());

        // 与平均水平对比
        analysis.put("comparisonWithAverage", generateComparisonWithAverage());

        return analysis;
    }

    @Override
    public Map<String, Object> exportExpenseList(Integer routeId, String format) {
        Map<String, Object> export = new HashMap<>();

        export.put("routeId", routeId);
        export.put("format", format);
        export.put("exportTime", new Date());
        export.put("fileName", generateExportFileName(routeId, format));
        export.put("status", "success");
        export.put("downloadUrl", "/api/budget/export/" + routeId + "/" + format);

        return export;
    }

    // 辅助方法
    private BigDecimal calculateOtherCost(Integer days, Integer travelerCount) {
        // 购物、娱乐等其他费用
        BigDecimal dailyOtherCost = new BigDecimal("100"); // 每人每天其他费用
        return dailyOtherCost.multiply(new BigDecimal(travelerCount))
                .multiply(new BigDecimal(days));
    }

    private List<Map<String, Object>> generateBudgetBreakdown(BigDecimal ticketCost,
                                                               BigDecimal transportCost,
                                                               BigDecimal accommodationCost,
                                                               BigDecimal mealCost,
                                                               BigDecimal otherCost,
                                                               BigDecimal totalCost) {
        List<Map<String, Object>> breakdown = new ArrayList<>();

        breakdown.add(createBreakdownItem("门票", ticketCost,
                ticketCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
        breakdown.add(createBreakdownItem("交通", transportCost,
                transportCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
        breakdown.add(createBreakdownItem("住宿", accommodationCost,
                accommodationCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
        breakdown.add(createBreakdownItem("餐饮", mealCost,
                mealCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
        breakdown.add(createBreakdownItem("其他", otherCost,
                otherCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));

        return breakdown;
    }

    private Map<String, Object> createBreakdownItem(String category, BigDecimal amount, BigDecimal percentage) {
        Map<String, Object> item = new HashMap<>();
        item.put("category", category);
        item.put("amount", amount);
        item.put("percentage", percentage);
        return item;
    }

    private Map<String, Object> createBudgetPlan(String type, String name, Integer routeId,
                                                  Integer travelerCount, Integer days,
                                                  String hotelLevel, String transportType, String mealLevel) {
        Map<String, Object> plan = new HashMap<>();

        BigDecimal ticketCost = calculateTicketCost(routeId, travelerCount);
        BigDecimal transportCost = calculateTransportCost(routeId, travelerCount, transportType);
        BigDecimal accommodationCost = calculateAccommodationCost(null, days, travelerCount, hotelLevel);
        BigDecimal mealCost = calculateMealCost(days, travelerCount, mealLevel);
        BigDecimal otherCost = calculateOtherCost(days, travelerCount);

        BigDecimal totalCost = ticketCost.add(transportCost).add(accommodationCost)
                .add(mealCost).add(otherCost);

        plan.put("type", type);
        plan.put("name", name);
        plan.put("ticketCost", ticketCost);
        plan.put("transportCost", transportCost);
        plan.put("accommodationCost", accommodationCost);
        plan.put("mealCost", mealCost);
        plan.put("otherCost", otherCost);
        plan.put("totalCost", totalCost);
        plan.put("perCapitaCost", totalCost.divide(new BigDecimal(travelerCount), 2, RoundingMode.HALF_UP));
        plan.put("description", generatePlanDescription(type));

        return plan;
    }

    private String generatePlanDescription(String type) {
        switch (type) {
            case "economy":
                return "经济型方案，适合预算有限的旅行者";
            case "luxury":
                return "豪华型方案，享受高品质旅行体验";
            default:
                return "舒适型方案，平衡性价比与体验";
        }
    }

    private Map<String, Object> generateCostDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("accommodation", 35);
        distribution.put("transport", 20);
        distribution.put("meals", 25);
        distribution.put("attractions", 15);
        distribution.put("others", 5);
        return distribution;
    }

    private Map<String, Object> generateComparisonWithAverage() {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("accommodation", "低于平均10%");
        comparison.put("transport", "与平均持平");
        comparison.put("meals", "高于平均5%");
        comparison.put("overall", "总体合理");
        return comparison;
    }

    private String generateExportFileName(Integer routeId, String format) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return "budget_plan_" + routeId + "_" + timestamp + "." + format.toLowerCase();
    }
}
