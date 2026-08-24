package travel.route.dto.route;

import java.time.LocalDate;
import java.util.List;

public record RouteVisitAnalyticsResponse(
        Integer routeId,
        LocalDate startDate,
        LocalDate endDate,
        long totalViews,
        long periodVisits,
        long uniqueVisitors,
        long returningVisitors,
        double retentionRate,
        List<RouteVisitTrendItem> trend) {
}
