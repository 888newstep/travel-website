# AIController DTO 深度执行清单

更新日期：2026-08-07

> 2026-08-20 状态说明：本文档保留为 DTO 治理历史记录。固定图片分析与伪多模态链路已整体删除，文中相关端点、Service 和 DTO 设计不再代表当前代码；现状以 `docs/showcase/CAPABILITY_BOUNDARIES.md` 为准。

本文档是 `BACKEND_EXECUTION_CHECKLIST.md` 的补充，聚焦于 **Phase 1 Batch 4** 的剩余工作：
将 `AIController` 中所有仍在使用 `Map<String, Object>` 作为入参/返回值的接口完成 DTO 化。

---

## 1. 当前状态盘点

### 1.1 已完成的 DTO 化端点（参考）

| 端点 | 请求 DTO | 响应 DTO | 状态 |
|------|----------|----------|------|
| `/ai/chat` | `ChatRequest`（内部类） | `AIChatResponse` | 响应已完成 |
| `/ai/advanced/chat` | `AdvancedChatRequest`（内部类） | `AIChatResponse` | 响应已完成 |
| `/ai/advanced/budget` | `AIEstimateBudgetRequest` | `AIBudgetResponse` | 全部完成 |
| `/ai/advanced/plan` | `AIPlanRouteRequest` | `Map<String, Object>` | 请求已完成 |
| `/ai/assistant/ask` | `AIQARequest` | `AIQARequest`（误用） | 需修正 |
| `/ai/itinerary/generate` | `AISmartItineraryGenerateRequest` | `AIItineraryResponseV2` | 全部完成 |
| `/ai/smart-itinerary/generate` | `AISmartItineraryGenerateRequest` | `AISmartItineraryResponse` | 全部完成 |
| `/ai/smart-itinerary/optimize` | `AISmartItineraryOptimizeRequest` | `AISmartItineraryOptimizeResponse` | 全部完成 |
| `/ai/advanced/guide` | `AIGenerateTravelGuideRequest` | `AITravelGuideResponse` | 全部完成 |

### 1.3 实际执行状态（2026-08-11）

- [x] 原 `AIController` 已按基础对话、助手、高级能力、图像、多模态拆分为多个职责单一的 Controller。
- [x] Controller 层不再使用 `Map<String, Object>` 作为 `@RequestBody`、返回值或 `Result` 泛型。
- [x] 请求 DTO 已接入 `jakarta.validation`，并通过 MockMvc 断言校验失败时不会调用 Service。
- [x] `AIAssistantService`、`AIAdvancedService` 的核心返回值已完成类型化；原图片占位和多模态 Service 后续已删除。
- [x] 新增 `AIAskQuestionRequest`，恢复 `POST /ai/assistant/ask` 的 JSON DTO 入参契约。
- [x] 恢复原 `POST /ai/assistant/optimize/{routeId}` 路径，并保留当前别名 `GET /ai/assistant/optimize-route/{routeId}`。
- [x] 删除占位链路后仅保留真实百度图像入口使用的 `AIAnalyzeImageResponse`，重复图片模型已清理。
- [x] 百度图像 Controller 已补充 MockMvc 安全断言；伪多模态 Controller 已删除，不再补充无效契约测试。
### 1.2 仍需 DTO 化的端点（本次目标）

| # | 端点 | 方法 | 当前请求类型 | 当前响应类型 | 复杂度 |
|---|------|------|-------------|-------------|--------|
| 1 | `/ai/assistant/chat` | `smartAssistant` | `AssistantQueryRequest`（内部类） | `Result<Map<String, Object>>` | 低 |
| 2 | `/ai/recommend` | `recommend` | `RecommendRequest`（内部类） | `Result<List<Map<String, Object>>>` | 中 |
| 3 | `/ai/advanced/recommendations` | `getPersonalizedRecommendations` | GET 参数 | `Result<List<Map<String, Object>>>` | 中 |
| 4 | `/ai/analyze` | `analyzeImage` | `ImageAnalysisRequest`（内部类） | `Result<Map<String, Object>>` | 中 |
| 5 | `/ai/qa` | `askQuestion` | `QARequest`（内部类） | `Result<Map<String, Object>>` | 低 |
| 6 | `/ai/optimize` | `optimizeRouteByAI` | `Map<String, Object>` | `Result<Map<String, Object>>` | 中 |
| 7 | `/ai/attraction/intro` | `getAttractionIntro` | `@RequestParam` | `Result<Map<String, Object>>` | 低 |
| 8 | `/ai/multimodal/query` | `multimodalQuery` | `MultimodalQueryRequest`（内部类） | `Result<Map<String, Object>>` | 高 |
| 9 | `/ai/advanced/plan` | `planRoute` | `AIPlanRouteRequest`（已独立） | `Result<Map<String, Object>>` | 中 |
| 10 | `/ai/safety/advice` | `getSafetyAdvice` | `@RequestParam` | `Result<Map<String, Object>>` | 低 |
| 11 | `/ai/multimodal/recommendations` | `getMultimodalRecommendations` | `MultipartFile` + 参数 | `Result<List<Map<String, Object>>>` | 高 |
| 12 | `/ai/multimodal/search` | `multimodalSearch` | `MultipartFile` + 参数 | `Result<List<Map<String, Object>>>` | 高 |

### 1.3 实际执行状态（2026-08-11）

- [x] 原 `AIController` 已按基础对话、助手、高级能力、图像、多模态拆分为职责单一的 Controller。
- [x] Controller 层不再使用 `Map<String, Object>` 作为 `@RequestBody`、返回值或 `Result` 泛型。
- [x] 请求 DTO 已接入 `jakarta.validation`，并通过 MockMvc 断言校验失败时不会调用 Service。
- [x] `AIAssistantService`、`AIAdvancedService` 的核心返回值已完成类型化；原图片占位和多模态 Service 后续已删除。
- [x] 新增 `AIAskQuestionRequest`，恢复 `POST /ai/assistant/ask` 的 JSON DTO 入参契约。
- [x] 恢复原 `POST /ai/assistant/optimize/{routeId}` 路径，并保留当前别名 `GET /ai/assistant/optimize-route/{routeId}`。
- [x] 删除占位链路后仅保留真实百度图像入口使用的 `AIAnalyzeImageResponse`，重复图片模型已清理。
- [x] 百度图像 Controller 已补充 MockMvc 安全断言；伪多模态 Controller 已删除，不再补充无效契约测试。

### 1.4 需要提取为独立 DTO 的内部类

当前 `AIController` 底部定义了 8 个 `static inner class` 请求 DTO，全部需要提取到 `dto/ai/` 目录：

| 内部类 | 使用端点 | 目标独立 DTO 名称 |
|--------|----------|-------------------|
| `ChatRequest` | `/ai/chat` | `AIChatRequest` |
| `AdvancedChatRequest` | `/ai/advanced/chat` | `AIAdvancedChatRequest` |
| `QARequest` | `/ai/qa` | 已有 `AIQARequest`，直接替换 |
| `AssistantQueryRequest` | `/ai/assistant/chat` | `AIAssistantChatRequest` |
| `RecommendRequest` | `/ai/recommend` | `AIRecommendRequest` |
| `ImageAnalysisRequest` | `/ai/analyze` | `AIAnalyzeImageRequest` |
| `ItineraryGenerateRequest` | `/ai/itinerary/generate` | 已有 `AISmartItineraryGenerateRequest`，评估合并 |
| `MultimodalQueryRequest` | `/ai/multimodal/query` | `AIMultimodalQueryRequest` |

---
## 2. 新建 DTO 详细设计

### 2.1 请求 DTO（从内部类提取 + 新增）

所有 DTO 位于 `travel.route.dto.ai` 包，使用 Lombok `@Data`，需要校验的字段使用 `jakarta.validation.constraints`。

#### AIChatRequest（提取自 `ChatRequest`）

```java
@Data
public class AIChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;
    private String systemPrompt;
}
```

#### AIAdvancedChatRequest（提取自 `AdvancedChatRequest`）

```java
@Data
public class AIAdvancedChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;
    private String conversationId;
}
```

#### AIAssistantChatRequest（提取自 `AssistantQueryRequest`）

```java
@Data
public class AIAssistantChatRequest {
    @NotBlank(message = "查询内容不能为空")
    private String query;
    // context 字段暂时保留 Map 类型，后续可细化为 typed 对象
    private Map<String, Object> context;
}
```

#### AIRecommendRequest（提取自 `RecommendRequest`）

```java
@Data
public class AIRecommendRequest {
    private Integer userId;
    private String location;
    private Map<String, Object> preferences;
    private Integer budget;
    @Min(value = 1, message = "时长至少1天")
    private Integer duration;
    private Integer cityId;
}
```

#### AIAnalyzeImageRequest（提取自 `ImageAnalysisRequest`）

```java
@Data
public class AIAnalyzeImageRequest {
    private String imageUrl;
    private String analysisType;
}
```

> 注意：`/ai/analyze` 端点同时支持 JSON 和 MultipartFile 两种模式，需要保留两个入口或统一为一种。

#### AIMultimodalQueryRequest（提取自 `MultimodalQueryRequest`）

```java
@Data
public class AIMultimodalQueryRequest {
    private String text;
    private String image;
    private Map<String, Object> context;
}
```

#### AIOptimizeRouteRequest（新增，替换 `Map<String, Object>`）

```java
@Data
public class AIOptimizeRouteRequest {
    @NotNull(message = "routeId不能为空")
    private Integer routeId;
    private Map<String, Object> preferences;
}
```
### 2.2 响应 DTO（全部新建）

#### AIAssistantChatResponse

来源端点：`/ai/assistant/chat`
数据来源：`qwenService.customerServiceReply()` + `extractSuggestions()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIAssistantChatResponse {
    private String response;
    private List<String> suggestions;
    private String source;
}
```

#### AIRecommendResponse

来源端点：`/ai/recommend`
数据来源：`qwenRecommendByAI()` / `fallbackRecommend()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIRecommendResponse {
    private List<AIRecommendationItem> items;
    // 可复用已有的 AIRecommendationItem
}
```

> 说明：`AIRecommendationItem` 已存在，包含 `id`, `name`, `description`, `matchScore`, `source` 字段，可直接复用。

#### AIPersonalizedRecommendationItem

来源端点：`/ai/advanced/recommendations`
数据来源：`AIAdvancedServiceImpl.getPersonalizedRecommendations()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIPersonalizedRecommendationItem {
    private Integer id;
    private String type;        // "attraction" | "restaurant" | "route" | "general"
    private String name;
    private String description;
    private Double rating;      // attractions/restaurants
    private Double distance;    // attractions
    private String priceLevel;  // restaurants
    private Integer days;       // routes
    private String difficulty;  // routes
    private Double score;
    private LocalDateTime recommendedAt;
}
```

> 说明：由于 `getPersonalizedRecommendations` 根据 `recommendationType` 返回不同字段结构，这里采用宽表设计（包含所有可能的字段），前端按 `type` 字段选择性使用。

#### AIAnalyzeImageResponse

来源端点：`/ai/analyze`
数据来源：`AIImageAnalysisServiceImpl.analyzeImageInternal()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIAnalyzeImageResponse {
    private Boolean success;
    private String analysisType;
    private LocalDateTime timestamp;
    private AIImageContentAnalysis contentAnalysis;
    private AIImageQualityAnalysis qualityAnalysis;
    private List<AIImageRecommendation> recommendations;
    private Double confidence;
    private String error;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIImageContentAnalysis {
    private String mainSubject;
    private List<String> objects;
    private String sceneType;
    private List<String> dominantColors;
    private String season;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIImageQualityAnalysis {
    private Double sharpness;
    private Double brightness;
    private Double contrast;
    private Double composition;
    private Double overallQuality;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIImageRecommendation {
    private String type;
    private String name;
    private List<String> items;
    private List<String> tips;
}
```
#### AIAskQuestionResponse

来源端点：`/ai/qa`
数据来源：`RealAIAssistantServiceImpl.askQuestion()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIAskQuestionResponse {
    private String question;
    private String answer;
    private Double confidence;
    private LocalDateTime timestamp;
    private String source;
}
```

#### AIOptimizeRouteResponse

来源端点：`/ai/optimize`
数据来源：`RealAIAssistantServiceImpl.optimizeRouteByAI()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIOptimizeRouteResponse {
    private Boolean success;
    private Integer routeId;
    private String suggestions;           // openai 模式为 String
    private List<AIOptimizeSuggestion> suggestionList; // fallback 模式为 List
    private Integer optimizedScore;
    private String source;
    private String message;               // 错误信息
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIOptimizeSuggestion {
    private String type;
    private String description;
}
```

> 说明：`optimizeRouteByAI` 在 openai 模式下 `suggestions` 是 String，在 fallback 模式下是 `List<Map>`。DTO 同时包含两种字段，前端按 `source` 判断使用哪个。

#### AIAttractionIntroResponse

来源端点：`/ai/attraction/intro`
数据来源：`RealAIAssistantServiceImpl.getAttractionIntro()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIAttractionIntroResponse {
    private Boolean success;
    private Integer attractionId;
    private String name;
    private String detailedIntro;
    private String bestVisitTime;
    private String source;
    private String message;  // 错误信息
}
```

#### AIMultimodalQueryResponse

来源端点：`/ai/multimodal/query`
数据来源：Controller 内部组装

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIMultimodalQueryResponse {
    private String textResponse;
    private Map<String, Object> imageAnalysis;
    private String combinedAnalysis;
    private String source;
}
```

#### AIPlanRouteResponse

来源端点：`/ai/advanced/plan`
数据来源：`AIAdvancedServiceImpl.planRoute()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIPlanRouteResponse {
    private Boolean success;
    private String planType;
    private LocalDateTime timestamp;
    private String destination;
    private Integer days;
    private String travelStyle;
    private List<AIDailyPlan> dailyPlans;
    private Integer estimatedCost;
    private Integer optimizationScore;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIDailyPlan {
    private Integer day;
    private String title;
    private List<AIActivity> activities;
    private Double totalCost;
    private Double totalDuration;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIActivity {
    private String time;
    private String type;
    private String name;
    private String description;
    private Double duration;
    private Double cost;
}
```
#### AISafetyAdviceResponse

来源端点：`/ai/safety/advice`
数据来源：`AIAdvancedServiceImpl.getSafetyAdvice()`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AISafetyAdviceResponse {
    private Boolean success;
    private Integer cityId;
    private String cityName;
    private List<String> safetyTips;
    private List<String> warnings;
    private List<String> emergencyContacts;
    private String source;
}
```

> 说明：`AIAdvancedServiceImpl.getSafetyAdvice()` 的具体字段需要查看实现确认，以上为推测结构，实现时按实际 Map 键调整。

#### AIMultimodalItem（复用型）

来源端点：`/ai/multimodal/recommendations` 和 `/ai/multimodal/search`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AIMultimodalItem {
    private Integer id;
    private String title;
    private String description;
    private Double score;      // recommendations 用
    private Double relevance;  // search 用
}
```

---

## 3. 分步执行计划

### Step 1：批量创建响应 DTO 文件

**目标**：在 `dto/ai/` 目录下创建所有新响应 DTO。
**文件清单**：

1. `AIAssistantChatResponse.java`
2. `AIRecommendResponse.java`
3. `AIPersonalizedRecommendationItem.java`
4. `AIAnalyzeImageResponse.java`
5. `AIImageContentAnalysis.java`
6. `AIImageQualityAnalysis.java`
7. `AIImageRecommendation.java`
8. `AIAskQuestionResponse.java`
9. `AIOptimizeRouteResponse.java`
10. `AIOptimizeSuggestion.java`
11. `AIAttractionIntroResponse.java`
12. `AIMultimodalQueryResponse.java`
13. `AIPlanRouteResponse.java`
14. `AIDailyPlan.java`
15. `AIActivity.java`
16. `AISafetyAdviceResponse.java`
17. `AIMultimodalItem.java`

**验证**：`mvn -pl route-service -am -DskipTests compile` 通过。

---

### Step 2：批量提取请求 DTO（内部类 → 独立文件）

**目标**：将 `AIController` 底部的 8 个 `static inner class` 提取为独立 DTO 文件。
**文件清单**：

1. `AIChatRequest.java`（替代 `ChatRequest`）
2. `AIAdvancedChatRequest.java`（替代 `AdvancedChatRequest`）
3. `AIAssistantChatRequest.java`（替代 `AssistantQueryRequest`）
4. `AIRecommendRequest.java`（替代 `RecommendRequest`）
5. `AIAnalyzeImageRequest.java`（替代 `ImageAnalysisRequest`）
6. `AIMultimodalQueryRequest.java`（替代 `MultimodalQueryRequest`）
7. `AIOptimizeRouteRequest.java`（新增）

**操作**：
1. 创建独立 DTO 文件，添加 `@NotBlank` / `@NotNull` / `@Min` 校验注解。
2. 修改 `AIController` 中对应方法的 `@RequestBody` 类型。
3. 在方法参数上添加 `@Valid` 注解。
4. 删除 `AIController` 底部的内部类定义。

**验证**：编译通过 + 确认所有端点 `@RequestBody` 类型已替换。
### Step 3：逐个端点转换响应类型（低复杂度组）

按复杂度从低到高排序，先处理结构简单的端点。

#### 3.1 `/ai/assistant/chat` → `AIAssistantChatResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> smartAssistant(@RequestBody AssistantQueryRequest request)

// After
public Result<AIAssistantChatResponse> smartAssistant(@Valid @RequestBody AIAssistantChatRequest request)
```

**改动逻辑**：
- 将 `Map.of("response", ..., "suggestions", ..., "source", ...)` 替换为 `AIAssistantChatResponse.builder()...build()`。
- `extractSuggestions()` 方法返回 `List<String>`，直接映射到 `AIAssistantChatResponse.suggestions`。

**Service 层影响**：无。`qwenService.customerServiceReply()` 返回 `String`，不需要改。

---

#### 3.2 `/ai/qa` → `AIAskQuestionResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> askQuestion(@RequestBody QARequest request)

// After
public Result<AIAskQuestionResponse> askQuestion(@Valid @RequestBody AIQARequest request)
```

**改动逻辑**：
- `aiAssistantService.askQuestion()` 返回 `Map<String, Object>`，在 Controller 层手动映射到 `AIAskQuestionResponse`。
- 映射字段：`question`, `answer`, `confidence`, `timestamp`, `source`。

**Service 层影响**：暂无。后续 Step 6 再改 Service 接口。

---

#### 3.3 `/ai/attraction/intro` → `AIAttractionIntroResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> getAttractionIntro(@RequestParam Integer attractionId)

// After
public Result<AIAttractionIntroResponse> getAttractionIntro(@RequestParam Integer attractionId)
```

**改动逻辑**：
- `aiAssistantService.getAttractionIntro()` 返回 `Map<String, Object>`，在 Controller 层映射。
- 映射字段：`success`, `attractionId`, `name`, `detailedIntro`, `bestVisitTime`, `source`, `message`。

---

#### 3.4 `/ai/safety/advice` → `AISafetyAdviceResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> getSafetyAdvice(@RequestParam Integer cityId)

// After
public Result<AISafetyAdviceResponse> getSafetyAdvice(@RequestParam Integer cityId)
```

**改动逻辑**：
- `aiAdvancedService.getSafetyAdvice()` 返回 `Map<String, Object>`，在 Controller 层映射。
### Step 4：逐个端点转换响应类型（中复杂度组）

#### 4.1 `/ai/recommend` → `AIRecommendResponse`

**Controller 改动**：
```java
// Before
public Result<List<Map<String, Object>>> recommend(@RequestBody RecommendRequest request)

// After
public Result<AIRecommendResponse> recommend(@Valid @RequestBody AIRecommendRequest request)
```

**改动逻辑**：
- `qwenRecommendByAI()` 返回 `List<Map<String, Object>>`，需要转换为 `List<AIRecommendationItem>`。
- 新增私有方法 `convertToRecommendationItems(List<Map<String, Object>>)` 做映射。
- 用 `AIRecommendResponse` 包装。

---

#### 4.2 `/ai/advanced/recommendations` → `List<AIPersonalizedRecommendationItem>`

**Controller 改动**：
```java
// Before
public Result<List<Map<String, Object>>> getPersonalizedRecommendations(...)

// After
public Result<List<AIPersonalizedRecommendationItem>> getPersonalizedRecommendations(...)
```

**改动逻辑**：
- `aiAdvancedService.getPersonalizedRecommendations()` 返回 `List<Map<String, Object>>`。
- 新增私有方法 `convertToPersonalizedItems()` 做映射。

---

#### 4.3 `/ai/optimize` → `AIOptimizeRouteResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> optimizeRouteByAI(@RequestBody Map<String, Object> requestBody)

// After
public Result<AIOptimizeRouteResponse> optimizeRouteByAI(@Valid @RequestBody AIOptimizeRouteRequest request)
```

**改动逻辑**：
- 入参从 `Map` 改为 `AIOptimizeRouteRequest`，用 `request.getRouteId()` 替代 `map.get("routeId")`。
- 返回值从 `Map` 映射到 `AIOptimizeRouteResponse`。

---

#### 4.4 `/ai/advanced/plan` → `AIPlanRouteResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> planRoute(@RequestBody AIPlanRouteRequest request)

// After
public Result<AIPlanRouteResponse> planRoute(@Valid @RequestBody AIPlanRouteRequest request)
```

**改动逻辑**：
- `aiAdvancedService.planRoute()` 返回复杂嵌套 `Map`，包含 `dailyPlans` → `activities`。
- 需要递归映射到 `AIPlanRouteResponse` → `AIDailyPlan` → `AIActivity`。
- 新增私有方法 `convertToPlanRouteResponse(Map<String, Object>)` 做深度映射。
### Step 5：逐个端点转换响应类型（高复杂度组）

#### 5.1 `/ai/analyze` → `AIAnalyzeImageResponse`

**Controller 改动**：
- 此端点有两个入口：JSON 请求和 `MultipartFile` 上传。
- JSON 入口使用 `AIAnalyzeImageRequest`。
- 文件入口保持 `MultipartFile`，但响应统一为 `AIAnalyzeImageResponse`。

**改动逻辑**：
- `aiImageAnalysisService.analyzeImage()` 返回深度嵌套 `Map`。
- 需要映射到 `AIAnalyzeImageResponse`，包含 `contentAnalysis`, `qualityAnalysis`, `recommendations`。
- 新增私有方法 `convertToAnalyzeResponse(Map<String, Object>)` 做深度映射。

---

#### 5.2 `/ai/multimodal/query` → `AIMultimodalQueryResponse`

**Controller 改动**：
```java
// Before
public Result<Map<String, Object>> multimodalQuery(@RequestBody MultimodalQueryRequest request)

// After
public Result<AIMultimodalQueryResponse> multimodalQuery(@Valid @RequestBody AIMultimodalQueryRequest request)
```

**改动逻辑**：
- Controller 内部组装结果，直接构建 `AIMultimodalQueryResponse`。
- 涉及 `baiduAIService` 和 `qwenService` 的调用结果合并。

---

#### 5.3 `/ai/multimodal/recommendations` → `List<AIMultimodalItem>`

**Controller 改动**：
- 保持 `MultipartFile` + `@RequestParam` 入参模式。
- 响应从 `List<Map<String, Object>>` 映射到 `List<AIMultimodalItem>`。

---

#### 5.4 `/ai/multimodal/search` → `List<AIMultimodalItem>`

**Controller 改动**：
- 同上，保持文件上传入参。
- 响应映射到 `List<AIMultimodalItem>`，使用 `relevance` 字段而非 `score`。

---

### Step 6：Service 接口类型传播（可选，推荐）

完成 Controller 层 DTO 化后，逐步将 Service 接口的 `Map<String, Object>` 返回值也替换为对应 DTO。

**优先级排序**：

1. `AIAssistantService.askQuestion()` → `AIAskQuestionResponse`
2. `AIAssistantService.getAttractionIntro()` → `AIAttractionIntroResponse`
3. `AIAssistantService.optimizeRouteByAI()` → `AIOptimizeRouteResponse`
4. `AIAdvancedService.getSafetyAdvice()` → `AISafetyAdviceResponse`
5. `AIAdvancedService.getPersonalizedRecommendations()` → `List<AIPersonalizedRecommendationItem>`
6. `AIAdvancedService.planRoute()` → `AIPlanRouteResponse`
7. `AIAdvancedService.generateTravelGuide()` → `AITravelGuideResponse`（已有 DTO）
8. `AIAdvancedService.estimateBudget()` → `AIBudgetResponse`（已有 DTO）
9. `AIImageAnalysisService.analyzeImage()` → `AIAnalyzeImageResponse`
10. `AIImageAnalysisService.recognizeAttraction()` → 新建 `AIRecognizeAttractionResponse`
11. `AIMultimodalService.getMultimodalRecommendations()` → `List<AIMultimodalItem>`
12. `AIMultimodalService.multimodalSearch()` → `List<AIMultimodalItem>`
13. `AISmartItineraryService.generateItinerary()` → `AISmartItineraryResponse`（已有 DTO）
14. `AISmartItineraryService.optimizeItinerary()` → `AISmartItineraryOptimizeResponse`（已有 DTO）

**原则**：
- 每改一个 Service 方法，同时改其 Impl 和 Controller 调用处。
- 改完后立即编译验证。
- Service 层使用 DTO 后，Controller 层不再需要手动 Map→DTO 转换代码。
### Step 7：清理 Controller 辅助方法

完成 DTO 化后，以下 Controller 内的辅助方法可以移除或简化：

| 方法 | 处置 |
|------|------|
| `parseMap(Object)` | 删除（不再需要 Map 解析） |
| `getIntegerValue(Map, String)` | 删除（DTO 化后不需要） |
| `getIntValue(Map, String, int)` | 删除 |
| `getDoubleValue(Map, String, double)` | 删除 |
| `buildPreferencesString(Map)` | 保留或移入 Service 层 |
| `extractSuggestions(String)` | 保留（仍被 `smartAssistant` 使用） |
| `downloadImageFromUrl(String)` | 保留（仍被 `analyzeImage` 使用） |
| `buildUserInput(AIRecommendRequest)` | 保留，更新参数类型 |
| `qwenRecommendByAI(...)` | 改为返回 `List<AIRecommendationItem>` |
| `fallbackRecommend(...)` | 改为返回 `List<AIRecommendationItem>` |

---

### Step 8：编译验证与回归测试

每个 Step 完成后执行：

```powershell
cd backend
mvn -pl route-service -am -DskipTests compile
```

全部完成后执行：

```powershell
cd backend
mvn -pl route-service -am test
```

---

## 4. 执行顺序依赖图

```
Step 1 (创建响应DTO)  ──┐
                         ├──> Step 3 (低复杂度端点) ──> Step 4 (中复杂度) ──> Step 5 (高复杂度)
Step 2 (提取请求DTO)  ──┘                                                          │
                                                                                    v
                                                                            Step 6 (Service传播)
                                                                                    │
                                                                                    v
                                                                            Step 7 (清理辅助方法)
                                                                                    │
                                                                                    v
                                                                            Step 8 (编译验证)
```

**关键路径**：Step 1 → Step 3 → Step 4 → Step 5 → Step 8
---

## 5. 风险与注意事项

### 5.1 前端兼容性

- DTO 化后 JSON 字段名可能与之前 Map 的 key 不完全一致。
- 需确认前端使用的字段名，必要时用 `@JsonProperty("old_name")` 保持兼容。
- 建议与前端同步确认每个端点的字段映射。

### 5.2 Service 层 Map 嵌套问题

- `AIAdvancedServiceImpl.planRoute()` 返回的 Map 包含 3 层嵌套（result → dailyPlans → activities）。
- 映射到 DTO 时需要逐层转换，不能简单 `BeanUtils.copy`。
- 建议在 Service 实现类中直接构建 DTO 对象，而不是在 Controller 层做深度映射。

### 5.3 文件上传端点的校验

- `MultipartFile` 端点不能使用 `@Valid @RequestBody` 做参数校验。
- 需要在方法体内手动校验文件类型、大小等。
- 考虑添加全局的文件上传校验拦截器。

### 5.4 Map 类型字段的处理

- 部分 DTO 仍包含 `Map<String, Object>` 字段（如 `preferences`, `context`）。
- 这些字段暂时保留 Map 类型，因为它们的内容是动态的。
- 后续可以根据实际使用场景，逐步将这些 Map 也替换为 typed 对象。

### 5.5 缓存兼容性

- `CacheUtil` 中缓存的旧数据是 Map 格式。
- Service 接口改为返回 DTO 后，需要清理相关缓存 key 或做缓存版本兼容。
- 涉及的缓存前缀：`ai:advanced:`, `ai:image:`, `ai:itinerary:`, `ai:qa:`。

---

## 6. 文件清单汇总

### 6.1 新建文件（17 个响应 DTO + 7 个请求 DTO = 24 个）

```
backend/route-service/src/main/java/travel/route/dto/ai/
├── AIActivity.java                    (新建)
├── AIAdvancedChatRequest.java         (新建)
├── AIAnalyzeImageRequest.java         (新建)
├── AIAnalyzeImageResponse.java        (新建)
├── AIAskQuestionRequest.java          (新建)
├── AIAskQuestionResponse.java         (新建)
├── AIAssistantChatRequest.java        (新建)
├── AIAssistantChatResponse.java       (新建)
├── AIAttractionIntroResponse.java     (新建)
├── AIChatRequest.java                 (新建)
├── AIDailyPlan.java                   (新建)
├── AIImageContentAnalysis.java        (新建)
├── AIImageQualityAnalysis.java        (新建)
├── AIImageRecommendation.java         (新建)
├── AIMultimodalItem.java              (新建)
├── AIMultimodalQueryRequest.java      (新建)
├── AIMultimodalQueryResponse.java     (新建)
├── AIOptimizeRouteRequest.java        (新建)
├── AIOptimizeRouteResponse.java       (新建)
├── AIOptimizeSuggestion.java          (新建)
├── AIPlanRouteResponse.java           (新建)
├── AIPersonalizedRecommendationItem.java (新建)
├── AIRecommendRequest.java            (新建)
├── AIRecommendResponse.java           (新建)
└── AISafetyAdviceResponse.java        (新建)
```

### 6.2 修改文件

```
backend/route-service/src/main/java/travel/route/controller/AIController.java
  - 替换所有 Map 响应为 typed DTO
  - 替换所有内部类请求为独立 DTO
  - 删除底部 8 个 static inner class
  - 清理不再需要的辅助方法

backend/route-service/src/main/java/travel/route/service/AIAssistantService.java
  - askQuestion() 返回值改为 AIAskQuestionResponse
  - optimizeRouteByAI() 返回值改为 AIOptimizeRouteResponse
  - getAttractionIntro() 返回值改为 AIAttractionIntroResponse

backend/route-service/src/main/java/travel/route/service/AIAdvancedService.java
  - getPersonalizedRecommendations() 返回值改为 List<AIPersonalizedRecommendationItem>
  - planRoute() 返回值改为 AIPlanRouteResponse
  - getSafetyAdvice() 返回值改为 AISafetyAdviceResponse

backend/route-service/src/main/java/travel/route/service/impl/RealAIAssistantServiceImpl.java
  - 对应方法返回值改为 typed DTO

backend/route-service/src/main/java/travel/route/service/impl/AIAdvancedServiceImpl.java
  - 对应方法返回值改为 typed DTO

backend/route-service/src/main/java/travel/route/service/impl/AIImageAnalysisServiceImpl.java
  - analyzeImage() 返回值改为 AIAnalyzeImageResponse (Step 6 时)

backend/route-service/src/main/java/travel/route/service/impl/AIMultimodalServiceImpl.java
  - 返回值改为 List<AIMultimodalItem> (Step 6 时)
```

---

## 7. 验收标准

1. `AIController` 及拆分后的 AI 子控制器中不再有任何 `Map<String, Object>` 作为 `@RequestBody` 或方法返回值。
2. 所有请求 DTO 都有 `jakarta.validation` 校验注解。
3. `AIController` 底部的 `static inner class` 全部清除。
4. `mvn compile` 零错误零警告（关于 unchecked cast）。
5. 所有端点的 JSON 响应结构与前端约定一致。
6. Service 接口（至少 `AIAssistantService` 和 `AIAdvancedService`）的返回值不再是 `Map`。

## 8. 本轮验收记录

- `mvn -q -pl route-service -am -DskipTests compile`：通过。
- `mvn -q -Dtest=AIAssistantControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`（在 `route-service` 模块执行）：通过，3 个测试全部通过。
- 已验证的结果断言：问答 DTO 字段映射、空问题校验前置、旧优化路由 POST 路径、新优化路由 GET 别名。
- 未宣称真实云端 RabbitMQ 业务链路已验收；本轮 AI DTO 改造不依赖 RabbitMQ，MySQL/Redis 仍按本机默认配置运行。
