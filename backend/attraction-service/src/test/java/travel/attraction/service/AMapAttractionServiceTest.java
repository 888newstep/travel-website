package travel.attraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import travel.attraction.dto.AMapNearbyFacilitiesResponse;
import travel.attraction.dto.AMapPlaceSearchResponse;
import travel.attraction.dto.AMapWeatherResponse;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.exception.BusinessException;
import travel.common.utils.AMapService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AMapAttractionServiceTest {

    private AMapService aMapService;
    private AttractionService attractionService;
    private AMapAttractionService service;

    @BeforeEach
    void setUp() {
        aMapService = mock(AMapService.class);
        attractionService = mock(AttractionService.class);
        service = new AMapAttractionService(aMapService, attractionService, new ObjectMapper());
    }

    @Test
    void shouldConvertAmapPoiToTypedCandidate() {
        Map<String, Object> poi = Map.of(
                "id", "B001",
                "name", "故宫博物院",
                "type", "风景名胜",
                "address", "景山前街4号",
                "pname", "北京市",
                "cityname", "北京市",
                "adname", "东城区",
                "location", "116.397026,39.918058",
                "photos", List.of(Map.of("url", "https://example.test/palace.jpg")));
        when(aMapService.searchPlaces("故宫", "北京", 1))
                .thenReturn(Map.of("count", 1, "pois", List.of(poi)));

        AMapPlaceSearchResponse response = service.searchPlaces(" 故宫 ", " 北京 ", 1);

        assertEquals(1, response.total());
        assertEquals(1, response.items().size());
        assertEquals("B001", response.items().get(0).poiId());
        assertEquals(116.397026, response.items().get(0).longitude());
        assertEquals("amap", response.items().get(0).source());
    }

    @Test
    void shouldUseControlledTypeCodeForNearbyFacilities() {
        Attraction attraction = new Attraction();
        attraction.setLongitude(BigDecimal.valueOf(116.397026));
        attraction.setLatitude(BigDecimal.valueOf(39.918058));
        when(attractionService.getById(1)).thenReturn(attraction);
        when(aMapService.getNearbyPlaces(116.397026, 39.918058, "150900", 1200))
                .thenReturn(Map.of("count", 0, "pois", List.of()));

        AMapNearbyFacilitiesResponse response = service.getNearbyFacilities(1, "parking", 1200);

        assertEquals("parking", response.category());
        assertEquals("停车场", response.categoryLabel());
        verify(aMapService).getNearbyPlaces(116.397026, 39.918058, "150900", 1200);
    }

    @Test
    void shouldRejectInvalidCategoryBeforeExternalCall() {
        assertThrows(BusinessException.class,
                () -> service.getNearbyFacilities(1, "arbitrary-amap-type", 1000));

        verifyNoInteractions(aMapService, attractionService);
    }

    @Test
    void shouldReportUnavailableWhenAttractionHasNoCoordinates() {
        when(attractionService.getById(1)).thenReturn(new Attraction());

        AMapNearbyFacilitiesResponse response = service.getNearbyFacilities(1, "restaurant", 1000);

        assertFalse(response.dataAvailable());
        assertEquals(List.of(), response.items());
        verifyNoInteractions(aMapService);
    }

    @Test
    void shouldExposeTypedWeatherForAttractionCoordinates() {
        Attraction attraction = new Attraction();
        attraction.setLongitude(BigDecimal.valueOf(116.397026));
        attraction.setLatitude(BigDecimal.valueOf(39.918058));
        when(attractionService.getById(1)).thenReturn(attraction);
        when(aMapService.getWeatherByLocation("116.397026,39.918058")).thenReturn(Map.of(
                "city", "东城区",
                "weather", "阴",
                "temperature", 26,
                "winddirection", "东",
                "windpower", "3",
                "humidity", "62"));

        AMapWeatherResponse response = service.getAttractionWeather(1);

        assertEquals("阴", response.weather());
        assertEquals(26, response.temperature());
        assertEquals("amap", response.source());
    }
}
