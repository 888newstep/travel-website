# -*- coding: utf-8 -*-
import io

path = r'C:\Users\xiaohongfu\IdeaProjects\travel\backend\route-service\src\main\java\travel\route\controller\AIController.java'
with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

failed = []

def run(old, new):
    global content
    if old not in content:
        failed.append(old[:120])
        return
    content = content.replace(old, new, 1)

# 1. replace imports
run("""import travel.route.dto.ai.AIChatResponse;
import travel.route.dto.ai.AIEstimateBudgetRequest;
import travel.route.dto.ai.AIPlanRouteRequest;
import travel.route.dto.ai.AIQARequest;
import travel.route.dto.ai.AIBudgetResponse;
import travel.route.dto.ai.AIGenerateTravelGuideRequest;
import travel.route.dto.ai.AISmartItineraryGenerateRequest;
import travel.route.dto.ai.AISmartItineraryOptimizeRequest;
import travel.common.utils.Result;
import travel.route.dto.ai.AITravelGuideResponse;
import travel.route.dto.ai.AISmartItineraryOptimizeResponse;
import travel.route.dto.ai.AISmartItineraryResponse;
import travel.route.dto.ai.AIItineraryResponseV2;
""", """import travel.route.dto.ai.*;
import travel.common.utils.Result;
""")

# 2. remove no-longer-used lombok.Data import
run("import lombok.Data;\n", "")

# 3. request DTO signatures
run("public Result<AIChatResponse> chat(@RequestBody ChatRequest request) {",
    "public Result<AIChatResponse> chat(@Valid @RequestBody AIChatRequest request) {")
run("public Result<AIChatResponse> advancedChatbot(@RequestBody AdvancedChatRequest request) {",
    "public Result<AIChatResponse> advancedChatbot(@Valid @RequestBody AIAdvancedChatRequest request) {")
run("public Result<Map<String, Object>> smartAssistant(@RequestBody AssistantQueryRequest request) {",
    "public Result<AIAssistantChatResponse> smartAssistant(@Valid @RequestBody AIAssistantChatRequest request) {")
run("public Result<List<Map<String, Object>>> recommend(@RequestBody RecommendRequest request) {",
    "public Result<List<AIRecommendationItem>> recommend(@Valid @RequestBody AIRecommendRequest request) {")
run("public Result<AIItineraryResponseV2> generateItinerary(@RequestBody ItineraryGenerateRequest request) {",
    "public Result<AIItineraryResponseV2> generateItinerary(@Valid @RequestBody AIItineraryGenerateRequest request) {")
run("public Result<Map<String, Object>> analyzeImage(@RequestBody ImageAnalysisRequest request) {",
    "public Result<AIAnalyzeImageResponse> analyzeImage(@Valid @RequestBody AIAnalyzeImageRequest request) {")
run("public Result<Map<String, Object>> multimodalQuery(@RequestBody MultimodalQueryRequest request) {",
    "public Result<AIMultimodalQueryResponse> multimodalQuery(@Valid @RequestBody AIMultimodalQueryRequest request) {")

print("Phase1 replacements done")
# 4. response DTO signatures and local variable types
run("public Result<List<Map<String, Object>>> getPersonalizedRecommendations(",
    "public Result<List<AIPersonalizedRecommendationItem>> getPersonalizedRecommendations(")
run("List<Map<String, Object>> recommendations = aiAdvancedService.getPersonalizedRecommendations(",
    "List<AIPersonalizedRecommendationItem> recommendations = aiAdvancedService.getPersonalizedRecommendations(")
run("List<Map<String, Object>> recommendations = qwenRecommendByAI(userInput, cityId, days, userId);",
    "List<AIRecommendationItem> recommendations = qwenRecommendByAI(userInput, cityId, days, userId);")
run("public Result<List<Map<String, Object>>> getSimilarAttractions(@RequestParam(\"file\") MultipartFile file,",
    "public Result<List<AISimilarAttractionItem>> getSimilarAttractions(@RequestParam(\"file\") MultipartFile file,")
run("List<Map<String, Object>> attractions = aiImageAnalysisService.getSimilarAttractions(file, limit);",
    "List<AISimilarAttractionItem> attractions = aiImageAnalysisService.getSimilarAttractions(file, limit);")
run("public Result<Map<String, Object>> askQuestion(@RequestBody AIQARequest request) {",
    "public Result<AIAskQuestionResponse> askQuestion(@Valid @RequestBody AIQARequest request) {")
run("Map<String, Object> answer = aiAssistantService.askQuestion(question, userId);",
    "AIAskQuestionResponse answer = aiAssistantService.askQuestion(question, userId);")
run("public Result<Map<String, Object>> optimizeRouteByAI(@PathVariable Integer routeId) {",
    "public Result<AIOptimizeRouteResponse> optimizeRouteByAI(@PathVariable Integer routeId) {")
run("Map<String, Object> optimization = aiAssistantService.optimizeRouteByAI(routeId);",
    "AIOptimizeRouteResponse optimization = aiAssistantService.optimizeRouteByAI(routeId);")
run("public Result<Map<String, Object>> getAttractionIntro(@PathVariable Integer attractionId) {",
    "public Result<AIAttractionIntroResponse> getAttractionIntro(@PathVariable Integer attractionId) {")
run("Map<String, Object> intro = aiAssistantService.getAttractionIntro(attractionId);",
    "AIAttractionIntroResponse intro = aiAssistantService.getAttractionIntro(attractionId);")
run("public Result<List<Map<String, Object>>> getMultimodalRecommendations(",
    "public Result<List<AIMultimodalItem>> getMultimodalRecommendations(")
run("List<Map<String, Object>> recommendations = aiMultimodalService.getMultimodalRecommendations(text, image, limit);",
    "List<AIMultimodalItem> recommendations = aiMultimodalService.getMultimodalRecommendations(text, image, limit);")
run("public Result<List<Map<String, Object>>> multimodalSearch(",
    "public Result<List<AIMultimodalItem>> multimodalSearch(")
run("List<Map<String, Object>> results = aiMultimodalService.multimodalSearch(text, image, page, size);",
    "List<AIMultimodalItem> results = aiMultimodalService.multimodalSearch(text, image, page, size);")
run("public Result<Map<String, Object>> planRoute(@RequestBody AIPlanRouteRequest request) {",
    "public Result<AIPlanRouteResponse> planRoute(@Valid @RequestBody AIPlanRouteRequest request) {")
run("Map<String, Object> route = aiAdvancedService.planRoute(preferences, constraints);",
    "AIPlanRouteResponse route = aiAdvancedService.planRoute(preferences, constraints);")
run("public Result<Map<String, Object>> getSafetyAdvice(@PathVariable Integer cityId) {",
    "public Result<AISafetyAdviceResponse> getSafetyAdvice(@PathVariable Integer cityId) {")
run("Map<String, Object> advice = aiAdvancedService.getSafetyAdvice(cityId);",
    "AISafetyAdviceResponse advice = aiAdvancedService.getSafetyAdvice(cityId);")
run("public Result<AIChatResponse> travelQA(@RequestBody AIQARequest request) {",
    "public Result<AIChatResponse> travelQA(@Valid @RequestBody AIQARequest request) {")
run("public Result<AIBudgetResponse> estimateBudget(@RequestBody AIEstimateBudgetRequest request) {",
    "public Result<AIBudgetResponse> estimateBudget(@Valid @RequestBody AIEstimateBudgetRequest request) {")

print("Phase2 replacements done")
# 5. getImageAnalysisTypes typed response
run("""    public Result<List<Map<String, String>>> getImageAnalysisTypes() {
        List<Map<String, String>> types = List.of(
                Map.of(\"value\", \"scene\", \"label\", \"场景识别\"),
                Map.of(\"value\", \"dish\", \"label\", \"菜品识别\"),
                Map.of(\"value\", \"ocr\", \"label\", \"文字识别\")
        );
        return Result.success(\"获取图像分析类型成功\", types);
    }""", """    public Result<List<AIImageAnalysisType>> getImageAnalysisTypes() {
        List<AIImageAnalysisType> types = List.of(
                new AIImageAnalysisType(\"scene\", \"场景识别\"),
                new AIImageAnalysisType(\"dish\", \"菜品识别\"),
                new AIImageAnalysisType(\"ocr\", \"文字识别\")
        );
        return Result.success(\"获取图像分析类型成功\", types);
    }""")

# 6. smartAssistant response builder
run("""            Map<String, Object> result = Map.of(
                    \"response\", response,
                    \"suggestions\", extractSuggestions(response),
                    \"source\", \"qwen\"
            );""", """            AIAssistantChatResponse result = AIAssistantChatResponse.builder()
                    .response(response)
                    .suggestions(extractSuggestions(response))
                    .source(\"qwen\")
                    .build();""")

# 7. multimodalQuery response builder
run("""            Map<String, Object> result = Map.of(
                    \"response\", response,
                    \"queryType\", \"multimodal\",
                    \"source\", \"qwen\"
            );""", """            AIMultimodalQueryResponse result = AIMultimodalQueryResponse.builder()
                    .response(response)
                    .queryType(\"multimodal\")
                    .source(\"qwen\")
                    .build();""")

# 8. analyzeImage response wrapper
run("""            return Result.success(\"图像分析成功\", result);""", """            AIAnalyzeImageResponse response = buildAnalyzeImageResponse(result, analysisType);
            return Result.success(\"图像分析成功\", response);""")

# 9. add analyze response helper before downloadImageFromUrl
run("""    private byte[] downloadImageFromUrl(String imageUrl) {""", """    private AIAnalyzeImageResponse buildAnalyzeImageResponse(Map<String, Object> result, String analysisType) {
        Map<String, Object> details = new java.util.HashMap<>(result);
        details.remove(\"success\");
        details.remove(\"error\");
        return AIAnalyzeImageResponse.builder()
                .success(Boolean.TRUE.equals(result.get(\"success\")))
                .analysisType(analysisType)
                .details(details)
                .error((String) result.get(\"error\"))
                .build();
    }

    private byte[] downloadImageFromUrl(String imageUrl) {""")

print("Phase3 replacements done")
# 10. recommend helpers use AIRecommendationItem
run("private String buildUserInput(RecommendRequest request) {",
    "private String buildUserInput(AIRecommendRequest request) {")
run("private List<Map<String, Object>> qwenRecommendByAI(String userInput, Integer cityId, int days, Integer userId) {",
    "private List<AIRecommendationItem> qwenRecommendByAI(String userInput, Integer cityId, int days, Integer userId) {")
run("""            Map<String, Object> recommendation = Map.of(
                    \"id\", 1,
                    \"name\", \"AI智能推荐路线\",
                    \"description\", aiResponse,
                    \"matchScore\", 95,
                    \"source\", \"qwen\"
            );""", """            AIRecommendationItem recommendation = AIRecommendationItem.builder()
                    .id(1)
                    .name(\"AI智能推荐路线\")
                    .description(aiResponse)
                    .matchScore(95)
                    .source(\"qwen\")
                    .build();""")
run("private List<Map<String, Object>> fallbackRecommend(String userInput) {",
    "private List<AIRecommendationItem> fallbackRecommend(String userInput) {")
run("""        Map<String, Object> route = Map.of(
                \"id\", 1,
                \"name\", \"推荐路线\",
                \"description\", \"基于您的需求: \" + userInput,
                \"matchScore\", 80,
                \"source\", \"fallback\"
        );
        return List.of(route);""", """        AIRecommendationItem route = AIRecommendationItem.builder()
                .id(1)
                .name(\"推荐路线\")
                .description(\"基于您的需求: \" + userInput)
                .matchScore(80)
                .source(\"fallback\")
                .build();
        return List.of(route);""")

# 11. remove unused Map helper methods
helper_block = '''    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(Object obj) {
        Map<String, Object> result = new java.util.HashMap<>();
        if (obj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

'''
run(helper_block, "")

# 12. remove inner request DTO classes
marker = "    // ==================== 请求DTO ===================="
if marker in content:
    idx = content.find(marker)
    end_idx = content.rfind("\n}")
    content = content[:idx].rstrip() + "\n}\n"
else:
    failed.append(marker)

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)

if failed:
    print("FAILED REPLACEMENTS:")
    for item in failed:
        print("-", item)
else:
    print("All replacements applied successfully")
print("Final file line count:", content.count(chr(10)) + 1)