package travel.attraction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import travel.attraction.dto.AMapNearbyFacilitiesResponse;
import travel.attraction.dto.AMapPlaceSearchResponse;
import travel.attraction.dto.AMapWeatherResponse;
import travel.attraction.service.AMapAttractionService;
import travel.common.utils.Result;

@RestController
@RequestMapping("/attractions")
@RequiredArgsConstructor
public class AMapAttractionController {

    private final AMapAttractionService aMapAttractionService;

    @GetMapping("/external-search")
    public Result<AMapPlaceSearchResponse> searchExternalPlaces(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "1") int page) {
        return Result.success("Fetched AMap place candidates successfully",
                aMapAttractionService.searchPlaces(keyword, city, page));
    }

    @GetMapping("/{id}/nearby-facilities")
    public Result<AMapNearbyFacilitiesResponse> getNearbyFacilities(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "restaurant") String category,
            @RequestParam(defaultValue = "1000") int radiusMeters) {
        return Result.success("Fetched AMap nearby facilities successfully",
                aMapAttractionService.getNearbyFacilities(id, category, radiusMeters));
    }

    @GetMapping("/{id}/weather")
    public Result<AMapWeatherResponse> getAttractionWeather(@PathVariable Integer id) {
        return Result.success("Fetched AMap weather successfully",
                aMapAttractionService.getAttractionWeather(id));
    }
}
