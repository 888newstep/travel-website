package travel.route.dto.route;

public record RouteTrafficSegmentResponse(
        String segmentId,
        Integer fromAttractionId,
        Integer toAttractionId,
        String status,
        long distanceMeters,
        long durationSeconds) {
}
