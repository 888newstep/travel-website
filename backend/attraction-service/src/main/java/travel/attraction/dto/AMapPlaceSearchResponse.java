package travel.attraction.dto;

import java.util.List;

public record AMapPlaceSearchResponse(
        boolean dataAvailable,
        String source,
        String keyword,
        String city,
        int page,
        int total,
        List<AMapPlaceCandidate> items,
        String message) {
}
