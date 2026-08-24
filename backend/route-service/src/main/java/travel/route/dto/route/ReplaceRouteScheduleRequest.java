package travel.route.dto.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReplaceRouteScheduleRequest {

    @NotNull
    @Size(max = 100)
    private List<@Valid RouteScheduleItemRequest> items = new ArrayList<>();
}
