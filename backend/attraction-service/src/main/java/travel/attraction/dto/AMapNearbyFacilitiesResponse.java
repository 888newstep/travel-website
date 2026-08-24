package travel.attraction.dto;

import java.util.List;

public record AMapNearbyFacilitiesResponse(
        boolean dataAvailable,
        String source,
        Integer attractionId,
        String category,
        String categoryLabel,
        int radiusMeters,
        List<AMapPlaceCandidate> items,
        String message) {
}
