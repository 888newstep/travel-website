# AI 接口精简 + TravelAnalytics 删除 清理计划

## Context（背景）

上一轮清理后，遗留两个待处理项：

1. **任务 A（精简 AI 接口）**：5 个 AI 服务接口共约 70 个方法，AIController 实际调用仅 22 个左右，过半未使用。需删除未使用方法以精简接口、消除死代码。
2. **任务 B（删除 TravelAnalyticsService）**：TravelAnalyticsServiceImpl 中 21 个方法全部返回 Mock 数据（Math.random、硬编码），与 RouteStatisticsService 有 7+ 项功能重叠。经前端项目检查，`frontend/src/api/` 目录中无 `analytics.api.ts`，App.vue 仅有一行注释引用，**无实际 HTTP 调用**。按用户指示"前端没调用就删除后端"，整体删除。

用户已确认两个关键决策：
- AIImageAnalysisService 中 `byte[]` 与 `MultipartFile` 两套重载：**删除 byte[] 版本**，仅保留 MultipartFile 版本
- AIAssistantService 中遗漏的 `recommendByAI` 方法：**一并删除**

预期成果：删除约 50 个未使用的 AI 接口方法、删除 3 个 TravelAnalytics 文件（21 个 Mock 方法和对应 API 端点），保持 AIController 端点行为完全不变。

## 任务 B：删除 TravelAnalyticsService（先执行，零依赖）

直接删除以下 3 个文件（已验证无外部引用、无测试依赖）：

- `src/main/java/travel/service/route_planning/TravelAnalyticsService.java`
- `src/main/java/travel/service/impl/route_planning/TravelAnalyticsServiceImpl.java`
- `src/main/java/travel/controller/route_planning_controller/TravelAnalyticsController.java`

完成后 `mvn compile` 验证。

## 任务 A：精简 AI 服务接口

按由简到繁顺序处理 5 个接口，每完成一个就编译一次。

### A1. AISmartItineraryService（最简单）

- 接口 `service/travel_recommendation/AISmartItineraryService.java`：删除 `adjustItinerary`、`generateAlternatives`、`predictSatisfaction`；保留 `generateItinerary`、`optimizeItinerary`
- 实现 `service/impl/travel_recommendation/AISmartItineraryServiceImpl.java`：删除上述 3 个方法实现 + 孤立辅助方法 `getAlternativeFocus`（仅被 generateAlternatives 调用）

### A2. AIMultimodalService（纯删除）

- 接口：删除 `multimodalChat`、`voiceInteraction`、`textImageInteraction`、`getSessionHistory`、`endSession`、`understandContent`、`getTextImageRecommendations`、`generateContent`、`compareContent`、`summarizeContent`、`analyzeSentiment`、`multimodalQA`、`getMultimodalReport`；保留 `getMultimodalRecommendations`、`multimodalSearch`
- 实现：删除上述 13 个方法实现
- 可选清理：删除后 `MULTIMODAL_PREFIX` 常量、`cacheUtil` 字段变为未使用，可一并删除

### A3. AIAdvancedService（纯删除 + 字段清理）

- 接口：删除 `chatWithAI`、`analyzeImage`、`translate`、`analyzeSentiment`、`answerQuestion`、`enhancedAttractionRecognition`、`optimizeItinerary`、`enhancedQuestionAnswering`、`getPersonalizedTravelAdvice`、`analyzeTravelHotspots`、`multimodalInteraction`；保留 `getPersonalizedRecommendations`、`processVoiceRequest`、`planRoute`、`generateTravelGuide`、`getSafetyAdvice`、`estimateBudget`
- 实现：删除上述 11 个方法实现
- 字段清理：`enhancedAttractionRecognition` 是该类中唯一使用 `aiServiceFactory` 字段的方法。删除后 `aiServiceFactory` 字段和 `import AIServiceFactory` 变为未使用，一并删除（不删除 AIServiceFactory 类本身，超出本任务范围）

### A4. AIAssistantService（同步 2 个实现）

- 接口：删除 `speechToText`、`textToSpeech`、`chatWithCustomerService`、`generateTravelDiary`、`getPhotoTips`、`getAudioGuide`、`summarizeTrip`、`predictBestTime`、`generatePackingList`、`analyzeSentiment`、`generateTags`、`recommendByAI`（共 12 个）；保留 `askQuestion`、`optimizeRouteByAI`、`getAttractionIntro`、`translate`
- 实现必须同步修改（删除完全相同的方法集合）：
  - `service/impl/travel_recommendation/RealAIAssistantServiceImpl.java`
  - `service/impl/travel_recommendation/RealQwenAssistantServiceImpl.java`

### A5. AIImageAnalysisService（最复杂，byte[]→private 转换）

**关键约束**：保留的 `analyzeImage(MultipartFile, String)` 内部调用 `analyzeImage(byte[], Integer)`，保留的 `recognizeAttraction(MultipartFile)` 内部调用 `recognizeAttraction(byte[])`。**这两个 byte[] 方法不能直接删除**，必须转为 private 辅助方法。

- 接口：删除所有 byte[] 重载声明 + 多余 MultipartFile 方法（`recognizeFood`、`generateImageDescription`、`analyzeImageQuality`、`batchAnalyzeImages` 两个重载、`assessImageQuality`、`analyzeImageColors`、`detectObjects`、`analyzeImageSentiment`）；保留 5 个 MultipartFile 方法
- 实现核心改造：
  1. `public Map<String, Object> analyzeImage(byte[], Integer)` → `private Map<String, Object> analyzeImageInternal(byte[], Integer)`，移除 `@Override`
  2. `public Map<String, Object> recognizeAttraction(byte[])` → `private Map<String, Object> recognizeAttractionInternal(byte[])`，移除 `@Override`
  3. 更新内部调用点：`analyzeImage(imageData, null)` → `analyzeImageInternal(imageData, null)`；`recognizeAttraction(imageData)` → `recognizeAttractionInternal(imageData)`
  4. 删除其他 byte[] 方法实现 + 多余 MultipartFile 方法实现
  5. 保留 `analyzeImage(MultipartFile)`、`recognizeAttraction(MultipartFile)`、`getSimilarAttractions`、`analyzeImageTags`、`getImageDescription`

## AIController 不修改

已验证 AIController 调用的全部方法都在保留清单内，无需任何修改。

## 任务 C：观察 mapper 和 repository 是否有新的孤立组件

完成任务 A/B 后，按用户要求扫描 mapper 和 repository 目录，检查是否因本次删除产生新的孤立组件：

- `src/main/java/travel/mapper/`：检查是否有 Mapper 不再被任何 Service 引用
- `src/main/java/travel/repository/`：检查是否有 Repository 不再被任何 Service 引用

如有，整理清单供用户决策是否删除。

## 验证方式

1. **任务 B 完成后**：`mvn compile`
2. **任务 A 每完成一个接口后**（A1→A5 各一次）：`mvn compile`
3. **全部完成后**：`mvn clean compile`（完整验证）
4. **AIController 端点回归**：编译通过后，确认 AIController 中所有 `aiAssistantService.xxx`、`aiAdvancedService.xxx`、`aiImageAnalysisService.xxx`、`aiMultimodalService.xxx`、`aiSmartItineraryService.xxx` 调用都指向保留方法

## 风险点

1. **AIImageAnalysisServiceImpl byte[] 内部调用**（A5）：必须先转为 private，否则编译失败
2. **AIAssistantService 两个实现必须同步**（A4）：RealAIAssistantServiceImpl 和 RealQwenAssistantServiceImpl 删除的方法集合必须完全一致
3. **接口与实现的 @Override 一致性**：删除接口方法后，实现中对应方法的 `@Override` 必须随方法体删除；转为 private 的方法必须移除 `@Override`
4. **删除方法时连同 Javadoc 一起删除**，避免遗留悬空注释
5. **不要修改 AIController.java**（已验证无需修改）
6. **不要删除 AIServiceFactory 类本身**（不在本任务范围）

## 关键文件清单

按修改复杂度排序（高→低）：

- `service/impl/travel_recommendation/AIImageAnalysisServiceImpl.java`（最复杂，byte[]→private）
- `service/impl/travel_recommendation/RealAIAssistantServiceImpl.java`（与 Qwen 实现同步）
- `service/impl/travel_recommendation/RealQwenAssistantServiceImpl.java`（与 OpenAI 实现同步）
- `service/impl/travel_recommendation/AISmartItineraryServiceImpl.java`（含孤立辅助方法）
- `service/travel_recommendation/AIAssistantService.java`（接口）
- `service/travel_recommendation/AIAdvancedService.java` + `AIAdvancedServiceImpl.java`
- `service/travel_recommendation/AIMultimodalService.java` + `AIMultimodalServiceImpl.java`
- `service/travel_recommendation/AISmartItineraryService.java`
- `service/travel_recommendation/AIImageAnalysisService.java`
