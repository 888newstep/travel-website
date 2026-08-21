package travel.route.service;

import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.dto.ai.AIOptimizeSuggestion;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AIAssistantResponseSupport {

    private AIAssistantResponseSupport() {
    }

    public static Map<String, Object> buildAskQuestionPayload(String question, String answer, String source) {
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("timestamp", LocalDateTime.now());
        result.put("source", source);
        return result;
    }

    public static AIAskQuestionResponse toAskQuestionResponse(Map<String, Object> source) {
        return AIAskQuestionResponse.builder()
                .question(asString(source.get("question")))
                .answer(asString(source.get("answer")))
                .confidence(asDouble(source.get("confidence")))
                .timestamp(asLocalDateTime(source.get("timestamp")))
                .source(asString(source.get("source")))
                .build();
    }

    public static AIOptimizeRouteResponse buildOptimizationSuccess(
            Integer routeId, String suggestion, String source) {
        return AIOptimizeRouteResponse.builder()
                .success(true)
                .routeId(routeId)
                .suggestions(List.of(AIOptimizeSuggestion.builder()
                        .type("summary")
                        .description(suggestion)
                        .build()))
                .optimizedScore(null)
                .source(source)
                .build();
    }

    public static Map<String, Object> buildAttractionIntroPayload(
            Integer attractionId, String name, String intro, String source) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("attractionId", attractionId);
        result.put("name", name);
        result.put("detailedIntro", intro);
        result.put("source", source);
        return result;
    }

    public static AIAttractionIntroResponse toAttractionIntroResponse(Map<String, Object> source) {
        return AIAttractionIntroResponse.builder()
                .success(asBoolean(source.get("success")))
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

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? null : Boolean.parseBoolean(value.toString());
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime timestamp) {
            return timestamp;
        }
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
