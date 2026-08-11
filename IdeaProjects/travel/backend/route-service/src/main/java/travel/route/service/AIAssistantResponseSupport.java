package travel.route.service;

import travel.common.entity.travel_recommendation.Attraction;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.dto.ai.AIOptimizeSuggestion;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AIAssistantResponseSupport {

    public static final String SOURCE_FALLBACK = "fallback";

    private static final String ROUTE_NOT_FOUND = "\u8def\u7ebf\u4e0d\u5b58\u5728";
    private static final String ATTRACTION_NOT_FOUND = "\u666f\u70b9\u4e0d\u5b58\u5728";
    private static final String DEFAULT_INTRO = "\u6682\u65e0\u8be6\u7ec6\u4ecb\u7ecd";
    private static final String DEFAULT_FUN_FACTS = "\u6682\u65e0\u8da3\u95fb";
    private static final String DEFAULT_BEST_VISIT_TIME = "\u5efa\u8bae\u767d\u5929\u524d\u5f80";
    private static final String DEFAULT_ESTIMATED_DURATION = "2-3\u5c0f\u65f6";
    private static final String DEFAULT_OPTIMIZE_SUGGESTION = "\u5efa\u8bae\u5408\u7406\u5b89\u6392\u65f6\u95f4\uff0c\u907f\u514d\u884c\u7a0b\u8fc7\u4e8e\u7d27\u51d1";
    private static final String DEFAULT_OPTIMIZE_MESSAGE = "\u5df2\u8fd4\u56de\u901a\u7528\u4f18\u5316\u5efa\u8bae";
    private static final String DEFAULT_INTRO_MESSAGE = "\u5df2\u8fd4\u56de\u57fa\u4e8e\u666f\u70b9\u57fa\u7840\u4fe1\u606f\u7684\u514e\u5e95\u4ecb\u7ecd";

    private AIAssistantResponseSupport() {
    }

    public static Map<String, Object> buildAskQuestionPayload(String question, String answer, double confidence, String source) {
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("confidence", confidence);
        result.put("timestamp", LocalDateTime.now());
        result.put("source", source);
        return result;
    }

    public static Map<String, Object> buildFallbackAskQuestionPayload(String question) {
        return buildAskQuestionPayload(question, resolveFallbackAnswer(question), 0.70, SOURCE_FALLBACK);
    }

    public static AIAskQuestionResponse toAskQuestionResponse(Map<String, Object> source) {
        return AIAskQuestionResponse.builder()
                .question(asString(source.get("question")))
                .answer(asString(source.get("answer")))
                .confidence(asDouble(source.get("confidence")))
                .timestamp(source.get("timestamp") instanceof LocalDateTime timestamp ? timestamp : null)
                .source(asString(source.get("source")))
                .build();
    }

    public static AIOptimizeRouteResponse buildOptimizationSuccess(Integer routeId, String suggestion, Integer optimizedScore, String source) {
        return AIOptimizeRouteResponse.builder()
                .success(true)
                .routeId(routeId)
                .suggestions(List.of(AIOptimizeSuggestion.builder()
                        .type("summary")
                        .description(suggestion)
                        .build()))
                .optimizedScore(optimizedScore)
                .source(source)
                .build();
    }

    public static AIOptimizeRouteResponse buildOptimizationFallback(Integer routeId, String errorMessage) {
        String message = errorMessage == null || errorMessage.isBlank()
                ? DEFAULT_OPTIMIZE_MESSAGE
                : DEFAULT_OPTIMIZE_MESSAGE + ": " + errorMessage;
        return AIOptimizeRouteResponse.builder()
                .success(true)
                .routeId(routeId)
                .suggestions(List.of(AIOptimizeSuggestion.builder()
                        .type("general")
                        .description(DEFAULT_OPTIMIZE_SUGGESTION)
                        .build()))
                .optimizedScore(75)
                .source(SOURCE_FALLBACK)
                .message(message)
                .build();
    }

    public static AIOptimizeRouteResponse buildRouteNotFound(Integer routeId) {
        return AIOptimizeRouteResponse.builder()
                .success(false)
                .routeId(routeId)
                .message(ROUTE_NOT_FOUND)
                .build();
    }

    public static Map<String, Object> buildAttractionIntroPayload(
            Integer attractionId,
            String name,
            String intro,
            String funFacts,
            String bestVisitTime,
            String estimatedDuration,
            String source
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("attractionId", attractionId);
        result.put("name", name);
        result.put("briefIntro", intro);
        result.put("detailedIntro", intro);
        result.put("funFacts", defaultIfBlank(funFacts, DEFAULT_FUN_FACTS));
        result.put("bestVisitTime", defaultIfBlank(bestVisitTime, DEFAULT_BEST_VISIT_TIME));
        result.put("estimatedDuration", defaultIfBlank(estimatedDuration, DEFAULT_ESTIMATED_DURATION));
        result.put("source", source);
        return result;
    }

    public static Map<String, Object> buildAttractionIntroFallbackPayload(Attraction attraction, Integer attractionId, String errorMessage) {
        String description = attraction != null ? defaultIfBlank(attraction.getDescription(), DEFAULT_INTRO) : DEFAULT_INTRO;
        Map<String, Object> result = buildAttractionIntroPayload(
                attractionId,
                attraction != null ? attraction.getName() : null,
                description,
                DEFAULT_FUN_FACTS,
                DEFAULT_BEST_VISIT_TIME,
                DEFAULT_ESTIMATED_DURATION,
                SOURCE_FALLBACK
        );
        result.put("message", errorMessage == null || errorMessage.isBlank()
                ? DEFAULT_INTRO_MESSAGE
                : DEFAULT_INTRO_MESSAGE + ": " + errorMessage);
        return result;
    }

    public static AIAttractionIntroResponse buildAttractionNotFound(Integer attractionId) {
        return AIAttractionIntroResponse.builder()
                .success(false)
                .attractionId(attractionId)
                .message(ATTRACTION_NOT_FOUND)
                .build();
    }

    public static AIAttractionIntroResponse toAttractionIntroResponse(Map<String, Object> source) {
        return AIAttractionIntroResponse.builder()
                .success(asBoolean(source.get("success"), true))
                .attractionId(asInteger(source.get("attractionId")))
                .name(asString(source.get("name")))
                .briefIntro(asString(source.get("briefIntro")))
                .detailedIntro(asString(source.get("detailedIntro")))
                .funFacts(asString(source.get("funFacts")))
                .bestVisitTime(asString(source.get("bestVisitTime")))
                .estimatedDuration(asString(source.get("estimatedDuration")))
                .source(asString(source.get("source")))
                .message(asString(source.get("message")))
                .build();
    }

    private static String resolveFallbackAnswer(String question) {
        String normalizedQuestion = question == null ? "" : question;
        if (normalizedQuestion.contains("\u5929\u6c14")) {
            return "\u5efa\u8bae\u5148\u67e5\u770b\u5f53\u5730\u5929\u6c14\u9884\u62a5\uff0c\u51fa\u53d1\u524d\u51c6\u5907\u9002\u5408\u5f53\u524d\u5b63\u8282\u7684\u8863\u7269\u3002";
        }
        if (normalizedQuestion.contains("\u95e8\u7968")) {
            return "\u5efa\u8bae\u4f18\u5148\u901a\u8fc7\u666f\u533a\u5b98\u65b9\u6e20\u9053\u6216\u4e3b\u6d41\u65c5\u6e38\u5e73\u53f0\u9884\u8ba2\u95e8\u7968\u3002";
        }
        if (normalizedQuestion.contains("\u4ea4\u901a")) {
            return "\u5e02\u5185\u51fa\u884c\u53ef\u4f18\u5148\u9009\u62e9\u5730\u94c1\u548c\u516c\u4ea4\uff0c\u8de8\u57ce\u53ef\u8003\u8651\u9ad8\u94c1\u6216\u98de\u673a\u3002";
        }
        if (normalizedQuestion.contains("\u4f4f\u5bbf")) {
            return "\u5efa\u8bae\u9009\u62e9\u4ea4\u901a\u4fbf\u5229\u4e14\u8bc4\u4ef7\u7a33\u5b9a\u7684\u4f4f\u5bbf\u533a\u57df\u3002";
        }
        if (normalizedQuestion.contains("\u7f8e\u98df") || normalizedQuestion.contains("\u5403")) {
            return "\u53ef\u4ee5\u4f18\u5148\u5c1d\u8bd5\u5f53\u5730\u7279\u8272\u5c0f\u5403\u548c\u53e3\u7891\u8f83\u597d\u7684\u8001\u5e97\u3002";
        }
        return "\u8bf7\u63d0\u4f9b\u66f4\u5177\u4f53\u7684\u65c5\u884c\u9700\u6c42\uff0c\u6211\u53ef\u4ee5\u7ed9\u51fa\u66f4\u6709\u9488\u5bf9\u6027\u7684\u5efa\u8bae\u3002";
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}