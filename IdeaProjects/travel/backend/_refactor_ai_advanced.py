# -*- coding: utf-8 -*-
import io

path = r'C:\Users\xiaohongfu\IdeaProjects\travel\backend\route-service\src\main\java\travel\route\service\impl\AIAdvancedServiceImpl.java'
with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

failed = []

def run(old, new):
    global content
    if old not in content:
        failed.append(old[:120])
        return
    content = content.replace(old, new, 1)

# imports
run("""import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.route.service.AIAdvancedService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;""", """import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.route.dto.ai.AIActivity;
import travel.route.dto.ai.AIDailyPlan;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AISafetyAdviceResponse;
import travel.route.service.AIAdvancedService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;""")

# getPersonalizedRecommendations return and types
run("public List<Map<String, Object>> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit) {",
    "public List<AIPersonalizedRecommendationItem> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit) {")
run("List<Map<String, Object>> recommendations = new ArrayList<>();",
    "List<AIPersonalizedRecommendationItem> recommendations = new ArrayList<>();")
run("List<Map<String, Object>> tempRecommendations = (List<Map<String, Object>>) cachedObj;",
    "List<AIPersonalizedRecommendationItem> tempRecommendations = (List<AIPersonalizedRecommendationItem>) cachedObj;")

print("advanced refactor phase1 done")
# getPersonalizedRecommendations loop body
run("""        for (int i = 1; i <= limit; i++) {
            Map<String, Object> recommendation = new HashMap<>();
            if (\"attractions\".equals(recommendationType)) {
                recommendation.put(\"id\", i);
                recommendation.put(\"type\", \"attraction\");
                recommendation.put(\"name\", \"热门景点\" + i);
                recommendation.put(\"description\", \"这是一个值得参观的热门景点\");
                recommendation.put(\"rating\", 4.5 + Math.random() * 0.5);
                recommendation.put(\"distance\", 10 + Math.random() * 20);
            } else if (\"restaurants\".equals(recommendationType)) {
                recommendation.put(\"id\", i);
                recommendation.put(\"type\", \"restaurant\");
                recommendation.put(\"name\", \"特色餐厅\" + i);
                recommendation.put(\"description\", \"这是一家提供当地特色美食的餐厅\");
                recommendation.put(\"rating\", 4.0 + Math.random() * 1.0);
                recommendation.put(\"priceLevel\", \"中等\");
            } else if (\"routes\".equals(recommendationType)) {
                recommendation.put(\"id\", i);
                recommendation.put(\"type\", \"route\");
                recommendation.put(\"name\", \"精选路线\" + i);
                recommendation.put(\"description\", \"这是一条精心设计的旅游路线\");
                recommendation.put(\"days\", 2 + (i % 3));
                recommendation.put(\"difficulty\", \"中等\");
            } else {
                recommendation.put(\"id\", i);
                recommendation.put(\"type\", \"general\");
                recommendation.put(\"name\", \"推荐项目\" + i);
                recommendation.put(\"description\", \"这是一个个性化推荐项目\");
            }
            recommendation.put(\"score\", 0.8 + Math.random() * 0.2);
            recommendation.put(\"recommendedAt\", LocalDateTime.now());
            recommendations.add(recommendation);
        }""", """        for (int i = 1; i <= limit; i++) {
            AIPersonalizedRecommendationItem recommendation;
            if (\"attractions\".equals(recommendationType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type(\"attraction\")
                        .name(\"热门景点\" + i)
                        .description(\"这是一个值得参观的热门景点\")
                        .rating(4.5 + Math.random() * 0.5)
                        .distance(10 + Math.random() * 20)
                        .score(0.8 + Math.random() * 0.2)
                        .recommendedAt(LocalDateTime.now())
                        .build();
            } else if (\"restaurants\".equals(recommendationType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type(\"restaurant\")
                        .name(\"特色餐厅\" + i)
                        .description(\"这是一家提供当地特色美食的餐厅\")
                        .rating(4.0 + Math.random() * 1.0)
                        .priceLevel(\"中等\")
                        .score(0.8 + Math.random() * 0.2)
                        .recommendedAt(LocalDateTime.now())
                        .build();
            } else if (\"routes\".equals(recommendationType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type(\"route\")
                        .name(\"精选路线\" + i)
                        .description(\"这是一条精心设计的旅游路线\")
                        .days(2 + (i % 3))
                        .difficulty(\"中等\")
                        .score(0.8 + Math.random() * 0.2)
                        .recommendedAt(LocalDateTime.now())
                        .build();
            } else {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type(\"general\")
                        .name(\"推荐项目\" + i)
                        .description(\"这是一个个性化推荐项目\")
                        .score(0.8 + Math.random() * 0.2)
                        .recommendedAt(LocalDateTime.now())
                        .build();
            }
            recommendations.add(recommendation);
        }""")

print("advanced refactor phase2 done")
# Rebuild planRoute method by replacing its whole method body range.
def replace_method(signature, new_method):
    global content
    start = content.find(signature)
    if start == -1:
        failed.append(signature[:120])
        return
    brace = content.find('{', start)
    depth = 1
    i = brace + 1
    while i < len(content) and depth > 0:
        if content[i] == '{':
            depth += 1
        elif content[i] == '}':
            depth -= 1
        i += 1
    end = i
    content = content[:start] + new_method + content[end:]

# planRoute
replace_method(
    "public Map<String, Object> planRoute(Map<String, Object> preferences, Map<String, Object> constraints) {",
    """    @Override
    public AIPlanRouteResponse planRoute(Map<String, Object> preferences, Map<String, Object> constraints) {
        String destination = preferences.getOrDefault("destination", "北京").toString();
        int days = (int) preferences.getOrDefault("days", 3);
        String travelStyle = preferences.getOrDefault("travelStyle", "balanced").toString();

        List<AIDailyPlan> dailyPlans = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            List<AIActivity> activities = new ArrayList<>();
            activities.add(new AIActivity("09:00-12:00", "attraction", "景点" + day + "-上午", "上午游览当地著名景点"));
            activities.add(new AIActivity("12:00-13:30", "restaurant", "餐厅" + day, "品尝当地特色美食"));
            activities.add(new AIActivity("14:00-17:00", "attraction", "景点" + day + "-下午", "下午参观文化景点"));
            dailyPlans.add(new AIDailyPlan(day, "第" + day + "天行程", activities));
        }

        return AIPlanRouteResponse.builder()
                .success(true)
                .planType("intelligent")
                .timestamp(LocalDateTime.now())
                .destination(destination)
                .days(days)
                .travelStyle(travelStyle)
                .dailyPlans(dailyPlans)
                .estimatedCost(1500 * days)
                .optimizationScore(85)
                .build();
    }
""")

print("planRoute replaced")
# getSafetyAdvice
replace_method(
    "public Map<String, Object> getSafetyAdvice(Integer cityId) {",
    """    @Override
    public AISafetyAdviceResponse getSafetyAdvice(Integer cityId) {
        String cityName = "北京";

        List<String> generalAdvice = new ArrayList<>();
        generalAdvice.add("保管好个人财物，尤其是在人多的地方");
        generalAdvice.add("随身携带身份证件，很多地方需要实名制");
        generalAdvice.add("注意交通安全，遵守交通规则");
        generalAdvice.add("关注天气变化，做好相应准备");
        generalAdvice.add("紧急情况可拨打110报警");

        List<String> travelAdvice = new ArrayList<>();
        travelAdvice.add("选择正规的旅行社和导游");
        travelAdvice.add("不要接受陌生人的搭讪和推销");
        travelAdvice.add("在景区内跟随指示牌，不要进入未开放区域");
        travelAdvice.add("注意饮食卫生，选择正规餐厅");
        travelAdvice.add("购买旅游保险，保障自身安全");

        Map<String, List<String>> areaAdvice = new HashMap<>();
        areaAdvice.put("景区", List.of("注意保管好门票和个人物品", "遵守景区规定，文明游览", "注意台阶和斜坡，防止摔倒"));
        areaAdvice.put("地铁", List.of("排队上下车，不要拥挤", "保管好随身物品", "注意站台间隙"));
        areaAdvice.put("商业区", List.of("注意扒手", "比较价格，避免被骗", "保管好购物凭证"));

        return AISafetyAdviceResponse.builder()
                .success(true)
                .cityId(cityId)
                .cityName(cityName)
                .advisedAt(LocalDateTime.now())
                .safetyLevel("high")
                .safetyScore(90)
                .generalAdvice(generalAdvice)
                .travelAdvice(travelAdvice)
                .areaAdvice(areaAdvice)
                .build();
    }
""")

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)

if failed:
    print("FAILED REPLACEMENTS:")
    for item in failed:
        print("-", item)
else:
    print("AIAdvancedServiceImpl refactored successfully")
print("Final file line count:", content.count(chr(10)) + 1)