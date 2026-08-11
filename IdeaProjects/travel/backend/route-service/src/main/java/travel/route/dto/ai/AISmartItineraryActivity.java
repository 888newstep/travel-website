package travel.route.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能行程中的单个活动。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISmartItineraryActivity {

    private String time;

    private String type;

    private String name;

    private String description;

    private Double duration;

    private Double cost;
}
