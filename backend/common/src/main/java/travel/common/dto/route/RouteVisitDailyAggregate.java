package travel.common.dto.route;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RouteVisitDailyAggregate {

    private LocalDate visitDate;

    private Long visits;

    private Long uniqueVisitors;
}
