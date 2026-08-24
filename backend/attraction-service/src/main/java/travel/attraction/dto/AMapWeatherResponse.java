package travel.attraction.dto;

public record AMapWeatherResponse(
        boolean dataAvailable,
        String source,
        Integer attractionId,
        String city,
        String weather,
        Integer temperature,
        String windDirection,
        String windPower,
        String humidity,
        String message) {
}
