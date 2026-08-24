package travel.route.dto.route;

import java.time.LocalDate;

public record RouteVisitTrendItem(
        LocalDate date,
        long visits,
        long uniqueVisitors) {
}
