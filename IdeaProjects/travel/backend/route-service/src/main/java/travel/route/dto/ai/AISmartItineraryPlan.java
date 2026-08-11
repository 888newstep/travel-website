package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智能行程生成结果。稳定业务字段使用 DTO，用户扩展偏好保留为 JsonNode。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISmartItineraryPlan {

    private Boolean success;

    private Integer userId;

    private Integer cityId;

    private Integer days;

    private Double budget;

    private Map<String, JsonNode> preferences;

    private Double estimatedCost;

    private Double estimatedDuration;

    private List<AISmartItineraryDayPlan> dailyPlans;

    private LocalDateTime generatedAt;

    private Integer optimizationScore;
}
