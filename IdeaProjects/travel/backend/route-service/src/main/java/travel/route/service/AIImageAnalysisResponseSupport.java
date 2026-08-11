package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import travel.route.dto.ai.AIImageAnalysisResponse;
import travel.route.dto.ai.AIImageContentAnalysis;
import travel.route.dto.ai.AIImageQualityAnalysis;
import travel.route.dto.ai.AIImageRecommendation;
import travel.route.dto.ai.AIRecognizeAttractionResponse;
import travel.route.dto.ai.AIRecognizedAttraction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AIImageAnalysisResponseSupport {

    private AIImageAnalysisResponseSupport() {
    }

    public static AIImageAnalysisResponse buildAnalysisFailure(String errorMessage) {
        return AIImageAnalysisResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();
    }

    public static AIRecognizeAttractionResponse buildRecognizeFailure(String errorMessage) {
        return AIRecognizeAttractionResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();
    }

    /**
     * 将第三方 AI 原始字段转换为受控的动态 JSON，避免 Map<String, Object> 进入对外响应。
     */
    public static Map<String, JsonNode> toDynamicDetails(Map<String, Object> result, ObjectMapper objectMapper) {
        if (result == null || result.isEmpty()) {
            return Map.of();
        }
        ObjectNode root = objectMapper.valueToTree(result);
        root.remove("success");
        root.remove("error");

        Map<String, JsonNode> details = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> details.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(details);
    }

    public static AIImageAnalysisResponse toImageAnalysisResponse(Map<String, Object> result) {
        if (result == null) {
            return buildAnalysisFailure(null);
        }
        return AIImageAnalysisResponse.builder()
                .success(Boolean.TRUE.equals(result.get("success")))
                .analysisType(asString(result.get("analysisType")))
                .timestamp(asLocalDateTime(result.get("timestamp")))
                .contentAnalysis(toContentAnalysis(result.get("contentAnalysis")))
                .qualityAnalysis(toQualityAnalysis(result.get("qualityAnalysis")))
                .recommendations(toImageRecommendations(result.get("recommendations")))
                .confidence(asDouble(result.get("confidence")))
                .error(asString(result.get("error")))
                .build();
    }

    public static AIRecognizeAttractionResponse toRecognizeAttractionResponse(Map<String, Object> result) {
        if (result == null) {
            return buildRecognizeFailure(null);
        }
        List<AIRecognizedAttraction> attractions = toRecognizedAttractions(result.get("attractions"));
        return AIRecognizeAttractionResponse.builder()
                .success(Boolean.TRUE.equals(result.get("success")))
                .type(asString(result.get("type")))
                .timestamp(asLocalDateTime(result.get("timestamp")))
                .attractions(attractions)
                .topMatch(result.get("topMatch") instanceof Map<?, ?> topMatchMap ? toRecognizedAttraction(topMatchMap) : null)
                .error(asString(result.get("error")))
                .build();
    }

    private static AIImageContentAnalysis toContentAnalysis(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return AIImageContentAnalysis.builder()
                .mainSubject(resolveMainSubject(map))
                .objects(toStringList(map.get("objects")))
                .sceneType(resolveSceneType(map))
                .dominantColors(resolveDominantColors(map))
                .season(asString(map.get("season")))
                .build();
    }

    private static AIImageQualityAnalysis toQualityAnalysis(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return AIImageQualityAnalysis.builder()
                .sharpness(asDouble(map.get("sharpness")))
                .brightness(asDouble(map.get("brightness")))
                .contrast(asDouble(map.get("contrast")))
                .composition(asDouble(map.get("composition")))
                .overallQuality(asDouble(map.get("overallQuality")))
                .build();
    }

    private static List<AIImageRecommendation> toImageRecommendations(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<AIImageRecommendation> recommendations = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                recommendations.add(AIImageRecommendation.builder()
                        .type(asString(map.get("type")))
                        .name(asString(map.get("name")))
                        .items(toStringList(map.get("items")))
                        .tips(toStringList(map.get("tips")))
                        .build());
            }
        }
        return recommendations;
    }

    private static List<AIRecognizedAttraction> toRecognizedAttractions(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<AIRecognizedAttraction> attractions = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                attractions.add(toRecognizedAttraction(map));
            }
        }
        return attractions;
    }

    private static AIRecognizedAttraction toRecognizedAttraction(Map<?, ?> map) {
        return AIRecognizedAttraction.builder()
                .name(asString(map.get("name")))
                .confidence(asDouble(map.get("confidence")))
                .location(asString(map.get("location")))
                .description(asString(map.get("description")))
                .rating(asDouble(map.get("rating")))
                .build();
    }

    private static String resolveMainSubject(Map<?, ?> map) {
        String mainSubject = asString(map.get("mainSubject"));
        if (mainSubject != null) {
            return mainSubject;
        }
        return asString(map.get("scene"));
    }

    private static String resolveSceneType(Map<?, ?> map) {
        String sceneType = asString(map.get("sceneType"));
        if (sceneType != null) {
            return sceneType;
        }
        return asString(map.get("scene"));
    }

    private static List<String> resolveDominantColors(Map<?, ?> map) {
        if (map.get("dominantColors") != null) {
            return toStringList(map.get("dominantColors"));
        }
        return toStringList(map.get("colors"));
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private static LocalDateTime asLocalDateTime(Object value) {
        return value instanceof LocalDateTime timestamp ? timestamp : null;
    }

    private static List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }
}
