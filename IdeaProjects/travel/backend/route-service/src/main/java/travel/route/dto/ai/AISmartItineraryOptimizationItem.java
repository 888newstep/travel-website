package travel.route.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条行程优化建议。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISmartItineraryOptimizationItem {

    private String type;

    private String description;

    private String benefit;

    private String priority;
}
