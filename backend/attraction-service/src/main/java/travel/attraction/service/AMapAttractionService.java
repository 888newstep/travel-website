package travel.attraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import travel.attraction.dto.AMapNearbyFacilitiesResponse;
import travel.attraction.dto.AMapPlaceCandidate;
import travel.attraction.dto.AMapPlaceSearchResponse;
import travel.attraction.dto.AMapWeatherResponse;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.utils.AMapService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AMapAttractionService {

    private static final int MIN_RADIUS_METERS = 100;
    private static final int MAX_RADIUS_METERS = 5000;
    private static final int MAX_PAGE = 20;

    private final AMapService aMapService;
    private final AttractionService attractionService;
    private final ObjectMapper objectMapper;

    public AMapPlaceSearchResponse searchPlaces(String keyword, String city, int page) {
        String normalizedKeyword = normalizeRequired(keyword, "keyword", 2, 50);
        String normalizedCity = normalizeOptional(city, "city", 20);
        if (page < 1 || page > MAX_PAGE) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "page must be between 1 and 20");
        }

        Map<String, Object> raw = aMapService.searchPlaces(normalizedKeyword, normalizedCity, page);
        if (raw == null) {
            return new AMapPlaceSearchResponse(false, "amap", normalizedKeyword, normalizedCity,
                    page, 0, List.of(), "高德地点搜索暂不可用");
        }
        return new AMapPlaceSearchResponse(true, "amap", normalizedKeyword, normalizedCity,
                page, number(raw.get("count")), parsePlaces(raw.get("pois")), null);
    }

    public AMapNearbyFacilitiesResponse getNearbyFacilities(
            Integer attractionId, String categoryValue, int radiusMeters) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(),
                    "radiusMeters must be between 100 and 5000");
        }
        AMapFacilityCategory category = AMapFacilityCategory.fromValue(categoryValue);
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        if (!hasValidCoordinates(attraction)) {
            return new AMapNearbyFacilitiesResponse(false, "amap", attractionId,
                    category.value(), category.label(), radiusMeters, List.of(), "景点缺少有效经纬度");
        }

        Map<String, Object> raw = aMapService.getNearbyPlaces(
                attraction.getLongitude().doubleValue(),
                attraction.getLatitude().doubleValue(),
                category.amapTypes(),
                radiusMeters);
        if (raw == null) {
            return new AMapNearbyFacilitiesResponse(false, "amap", attractionId,
                    category.value(), category.label(), radiusMeters, List.of(), "高德周边设施暂不可用");
        }
        return new AMapNearbyFacilitiesResponse(true, "amap", attractionId,
                category.value(), category.label(), radiusMeters, parsePlaces(raw.get("pois")), null);
    }

    public AMapWeatherResponse getAttractionWeather(Integer attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }
        if (!hasValidCoordinates(attraction)) {
            return unavailableWeather(attractionId, "景点缺少有效经纬度");
        }
        String location = attraction.getLongitude().toPlainString() + "," + attraction.getLatitude().toPlainString();
        Map<String, Object> raw = aMapService.getWeatherByLocation(location);
        if (raw == null) {
            return unavailableWeather(attractionId, "高德天气暂不可用");
        }
        return new AMapWeatherResponse(
                true,
                "amap",
                attractionId,
                string(raw.get("city")),
                string(raw.get("weather")),
                nullableInteger(raw.get("temperature")),
                string(raw.get("winddirection")),
                string(raw.get("windpower")),
                string(raw.get("humidity")),
                null);
    }

    private List<AMapPlaceCandidate> parsePlaces(Object rawPois) {
        JsonNode pois = objectMapper.valueToTree(rawPois);
        if (!pois.isArray()) {
            return List.of();
        }
        List<AMapPlaceCandidate> items = new ArrayList<>();
        for (JsonNode poi : pois) {
            AMapPlaceCandidate candidate = parsePlace(poi);
            if (candidate != null) {
                items.add(candidate);
            }
        }
        return List.copyOf(items);
    }

    private AMapPlaceCandidate parsePlace(JsonNode poi) {
        String name = text(poi, "name");
        double[] location = parseLocation(text(poi, "location"));
        if (name == null || location == null) {
            return null;
        }
        JsonNode photos = poi.path("photos");
        String imageUrl = photos.isArray() && !photos.isEmpty() ? text(photos.get(0), "url") : null;
        return new AMapPlaceCandidate(
                text(poi, "id"),
                name,
                text(poi, "type"),
                text(poi, "address"),
                text(poi, "pname"),
                text(poi, "cityname"),
                text(poi, "adname"),
                location[0],
                location[1],
                nullableNumber(poi.get("distance")),
                imageUrl,
                "amap");
    }

    private boolean hasValidCoordinates(Attraction attraction) {
        if (attraction.getLongitude() == null || attraction.getLatitude() == null) {
            return false;
        }
        double longitude = attraction.getLongitude().doubleValue();
        double latitude = attraction.getLatitude().doubleValue();
        return longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90;
    }

    private String normalizeRequired(String value, String field, int minLength, int maxLength) {
        String normalized = normalizeOptional(value, field, maxLength);
        if (normalized.length() < minLength) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(),
                    field + " length must be between " + minLength + " and " + maxLength);
        }
        return normalized;
    }

    private String normalizeOptional(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), field + " is invalid");
        }
        return normalized;
    }

    private double[] parseLocation(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double longitude = Double.parseDouble(parts[0]);
            double latitude = Double.parseDouble(parts[1]);
            if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
                return null;
            }
            return new double[]{longitude, latitude};
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || value.isContainerNode()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() || "[]".equals(text) ? null : text;
    }

    private Integer nullableNumber(JsonNode value) {
        if (value == null || value.isNull() || value.isContainerNode() || value.asText().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private AMapWeatherResponse unavailableWeather(Integer attractionId, String message) {
        return new AMapWeatherResponse(false, "amap", attractionId,
                null, null, null, null, null, null, message);
    }

    private Integer nullableInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
