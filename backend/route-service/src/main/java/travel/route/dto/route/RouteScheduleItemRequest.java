package travel.route.dto.route;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RouteScheduleItemRequest {

    @NotNull
    @Positive
    private Integer attractionId;

    @NotNull
    @Positive
    private Integer dayNumber;

    @NotNull
    @Positive
    private Integer visitOrder;

    @Size(max = 500)
    private String notes;
}
