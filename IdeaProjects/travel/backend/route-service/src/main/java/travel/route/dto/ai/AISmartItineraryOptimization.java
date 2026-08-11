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
 * 现有路线的智能优化结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISmartItineraryOptimization {

    private Boolean success;

    private Integer routeId;

    private String routeTitle;

    private String cityName;

    private Map<String, JsonNode> preferences;

    private List<AISmartItineraryOptimizationItem> optimizations;

    private Integer optimizedScore;

    private Double estimatedSavings;

    private LocalDateTime optimizedAt;
}
