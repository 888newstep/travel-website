package travel.attraction.dto;

public record AMapPlaceCandidate(
        String poiId,
        String name,
        String type,
        String address,
        String province,
        String city,
        String district,
        Double longitude,
        Double latitude,
        Integer distanceMeters,
        String imageUrl,
        String source) {
}
