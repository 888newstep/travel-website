package travel.route.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能行程中的单日计划。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISmartItineraryDayPlan {

    private Integer day;

    private String title;

    private List<AISmartItineraryActivity> activities;

    private Double totalCost;

    private Double totalDuration;
}
