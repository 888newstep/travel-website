from pathlib import Path
import re

path = Path(r'C:\Users\xiaohongfu\IdeaProjects\travel\backend\route-service\src\main\java\travel\route\controller\AIController.java')
text = path.read_text(encoding='utf-8')

imports = [
    'import travel.route.dto.ai.AIChatResponse;',
    'import travel.route.dto.ai.AIEstimateBudgetRequest;',
    'import travel.route.dto.ai.AIPlanRouteRequest;',
    'import travel.route.dto.ai.AIQARequest;',
    'import travel.route.dto.ai.AIBudgetResponse;',
    'import travel.route.dto.ai.AIGenerateTravelGuideRequest;',
    'import travel.route.dto.ai.AISmartItineraryGenerateRequest;',
    'import travel.route.dto.ai.AISmartItineraryOptimizeRequest;',
    'import travel.route.dto.ai.AIItineraryResponseV2;',
    'import travel.route.dto.ai.AISmartItineraryResponse;',
    'import travel.route.dto.ai.AISmartItineraryOptimizeResponse;',
    'import travel.route.dto.ai.AITravelGuideResponse;',
]
for imp in imports:
    if imp not in text:
        text = text.replace('import travel.common.utils.Result;\n', f'import travel.common.utils.Result;\n{imp}\n')

text = re.sub(
    r'@PostMapping\("/itinerary/generate"\)\s*@Operation\(summary = ".*?", description = ".*?"\)\s*public Result<Map<String, Object>> generateItinerary\(@RequestBody ItineraryGenerateRequest request\) \{.*?\n    \}\n\n    @PostMapping\("/smart-itinerary/generate"\)',
    '''@PostMapping("/itinerary/generate")
    @Operation(summary = "生成行程", description = "使用通义千问生成旅行行程")
    public Result<AIItineraryResponseV2> generateItinerary(@RequestBody ItineraryGenerateRequest request) {
        try {
            log.info("生成行程请求: destination={}, days={}", request.getDestination(), request.getDays());

            String preferences = buildPreferencesString(request.getPreferences());
            String budget = request.getBudget() != null ? "楼" + request.getBudget() : "中等预算";

            String itineraryJson = qwenService.recommendItinerary(preferences, request.getDays(), budget);
            AIItineraryResponseV2 result = AIItineraryResponseV2.builder()
                    .destination(request.getDestination())
                    .days(request.getDays())
                    .itinerary(itineraryJson)
                    .source("qwen")
                    .build();

            return Result.success("行程生成成功", result);
        } catch (Exception e) {
            log.error("行程生成失败: {}", e.getMessage(), e);
            return Result.error("行程生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/smart-itinerary/generate")''',
    text,
    flags=re.S,
)

text = re.sub(
    r'@PostMapping\("/smart-itinerary/generate"\)\s*@Operation\(summary = ".*?", description = ".*?"\)\s*public Result<Map<String, Object>> generateSmartItinerary\(@Valid @RequestBody AISmartItineraryGenerateRequest itineraryRequest\) \{.*?\n    \}\n\n    @PostMapping\("/smart-itinerary/optimize"\)',
    '''@PostMapping("/smart-itinerary/generate")
    @Operation(summary = "生成智能行程", description = "基于AI生成智能行程")
    public Result<AISmartItineraryResponse> generateSmartItinerary(@Valid @RequestBody AISmartItineraryGenerateRequest itineraryRequest) {
        try {
            log.info("生成智能行程请求: userId={}, cityId={}", itineraryRequest.getUserId(), itineraryRequest.getCityId());
            Integer userId = itineraryRequest.getUserId();
            Integer cityId = itineraryRequest.getCityId();
            int days = itineraryRequest.getDays() != null ? itineraryRequest.getDays() : 3;
            double budget = itineraryRequest.getBudget() != null ? itineraryRequest.getBudget() : 1000.0;
            Map<String, Object> userPreferences = itineraryRequest.getPreferences() == null ? Map.of() : itineraryRequest.getPreferences();

            Map<String, Object> itinerary = aiSmartItineraryService.generateItinerary(userPreferences, budget, days, cityId, userId);
            AISmartItineraryResponse response = AISmartItineraryResponse.builder()
                    .userId(userId)
                    .cityId(cityId)
                    .days(days)
                    .budget(budget)
                    .itinerary(itinerary)
                    .source("ai-smart-itinerary")
                    .build();
            return Result.success("生成智能行程成功", response);
        } catch (Exception e) {
            log.error("生成智能行程失败: {}", e.getMessage(), e);
            return Result.error("生成智能行程失败: " + e.getMessage());
        }
    }

    @PostMapping("/smart-itinerary/optimize")''',
    text,
    flags=re.S,
)

text = re.sub(
    r'@PostMapping\("/smart-itinerary/optimize"\)\s*@Operation\(summary = ".*?", description = ".*?"\)\s*public Result<Map<String, Object>> optimizeItinerary\(@Valid @RequestBody AISmartItineraryOptimizeRequest itineraryData\) \{.*?\n    \}\n\n    // ==================== .*?\n\n    @GetMapping\("/image-analysis/types"\)',
    '''@PostMapping("/smart-itinerary/optimize")
    @Operation(summary = "优化行程", description = "优化已有行程")
    public Result<AISmartItineraryOptimizeResponse> optimizeItinerary(@Valid @RequestBody AISmartItineraryOptimizeRequest itineraryData) {
        try {
            log.info("优化行程请求: routeId={}", itineraryData.getRouteId());
            Integer routeId = itineraryData.getRouteId();
            Map<String, Object> userPreferences = itineraryData.getPreferences() == null ? Map.of() : itineraryData.getPreferences();

            Map<String, Object> optimized = aiSmartItineraryService.optimizeItinerary(routeId, userPreferences);
            AISmartItineraryOptimizeResponse response = AISmartItineraryOptimizeResponse.builder()
                    .routeId(routeId)
                    .optimized(optimized)
                    .source("ai-smart-itinerary")
                    .build();
            return Result.success("优化行程成功", response);
        } catch (Exception e) {
            log.error("优化行程失败: {}", e.getMessage(), e);
            return Result.error("优化行程失败: " + e.getMessage());
        }
    }

    // ==================== 图像分析 ====================

    @GetMapping("/image-analysis/types")''',
    text,
    flags=re.S,
)

text = re.sub(
    r'@PostMapping\("/advanced/guide"\)\s*@Operation\(summary = ".*?", description = ".*?"\)\s*public Result<Map<String, Object>> generateTravelGuide\(@Valid @RequestBody AIGenerateTravelGuideRequest request\) \{.*?\n    \}\n\n    @GetMapping\("/advanced/safety/\{cityId\}"\)',
    '''@PostMapping("/advanced/guide")
    @Operation(summary = "生成旅游攻略", description = "生成旅游攻略")
    public Result<AITravelGuideResponse> generateTravelGuide(@Valid @RequestBody AIGenerateTravelGuideRequest request) {
        try {
            Integer cityId = request.getCityId();
            Integer days = request.getDays();
            Map<String, Object> preferences = request.getPreferences() == null ? Map.of() : request.getPreferences();
            log.info("生成旅游攻略请求: cityId={}, days={}", cityId, days);
            Map<String, Object> guide = aiAdvancedService.generateTravelGuide(cityId, days, preferences);
            AITravelGuideResponse response = AITravelGuideResponse.builder()
                    .cityId(cityId)
                    .days(days)
                    .guide(guide)
                    .source("ai-advanced")
                    .build();
            return Result.success("生成旅游攻略成功", response);
        } catch (Exception e) {
            log.error("生成旅游攻略失败: {}", e.getMessage(), e);
            return Result.error("生成旅游攻略失败: " + e.getMessage());
        }
    }

    @GetMapping("/advanced/safety/{cityId}")''',
    text,
    flags=re.S,
)

path.write_text(text, encoding='utf-8', newline='\n')
print('updated controller responses')
