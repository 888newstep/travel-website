package travel.attraction.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_recommendation.AttractionReview;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.mapper.travel_recommendation_mapper.AttractionReviewMapper;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.attraction.service.AttractionService;
import travel.common.utils.CacheUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assertions;
import static org.mockito.Mockito.*;

/**
 * 验证 AttractionDetailServiceImpl 的点评落库与周边景点逻辑。
 */
class AttractionDetailServiceImplTest {

    private AttractionService attractionService;
    private CacheUtil cacheUtil;
    private AttractionReviewMapper reviewMapper;
    private AttractionRealtimeStatusMapper realtimeStatusMapper;
    private AttractionDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        attractionService = mock(AttractionService.class);
        cacheUtil = mock(CacheUtil.class);
        reviewMapper = mock(AttractionReviewMapper.class);
        realtimeStatusMapper = mock(AttractionRealtimeStatusMapper.class);
        service = new AttractionDetailServiceImpl(attractionService, cacheUtil, reviewMapper, realtimeStatusMapper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Attraction attraction(int id, int cityId, String name, double lat, double lng) {
        Attraction a = new Attraction();
        a.setId(id);
        a.setCityId(cityId);
        a.setName(name);
        a.setLatitude(BigDecimal.valueOf(lat));
        a.setLongitude(BigDecimal.valueOf(lng));
        return a;
    }

    @Test
    void getAttractionReviews_returnsMappedReviewsFromMapper() throws Exception {
        AttractionReview review = new AttractionReview();
        review.setId(10);
        review.setAttractionId(5);
        review.setUserId(3);
        review.setRating(4);
        review.setContent("不错");
        review.setCreatedAt(java.time.LocalDateTime.of(2026, 8, 10, 12, 0));

        Page<AttractionReview> reviewPage = new Page<>();
        reviewPage.setRecords(List.of(review));
        when(reviewMapper.selectPage(any(), any())).thenReturn(reviewPage);

        List<Map<String, Object>> result = service.getAttractionReviews(5L, 0, 10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).get("id"));
        assertEquals(4, result.get(0).get("rating"));
        assertEquals("不错", result.get(0).get("content"));
        assertNotNull(result.get(0).get("createTime"));
        verify(reviewMapper, times(1)).selectPage(any(), any());
    }

    @Test
    void saveAttractionReview_insertsAndReturnsReview() {
        when(attractionService.getById(7)).thenReturn(new Attraction());
        when(reviewMapper.upsertReview(any(AttractionReview.class))).thenAnswer(inv -> {
            AttractionReview r = inv.getArgument(0);
            r.setId(99);  // 模拟自增回填
            return 1;
        });

        Map<String, Object> result = service.saveAttractionReview(7, 2, 5, "很好");

        assertEquals(99, result.get("id"));
        assertEquals(7, result.get("attractionId"));
        assertEquals(42, result.get("userId"));
        assertEquals(5, result.get("rating"));
        assertNotNull(result.get("createTime"));
        verify(reviewMapper, times(1)).upsertReview(any(AttractionReview.class));
    }

    @Test
    void getNearbyAttractions_excludesSelfAndSortsByDistance() {
        Attraction current = attraction(1, 10, "故宫", 39.9163, 116.3972);
        Attraction near = attraction(2, 10, "天安门", 39.9087, 116.3975);      // 约 0.85km
        Attraction far = attraction(3, 10, "香山", 39.9915, 116.1852);        // 约 20km
        when(attractionService.getById(1)).thenReturn(current);
        when(attractionService.getByCityId(10)).thenReturn(Arrays.asList(current, near, far));

        List<Map<String, Object>> result = service.getNearbyAttractions(1, 5);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).get("id"));  // 排除自身后，最近的在前
        assertEquals(3, result.get(1).get("id"));
        assertTrue(((Double) result.get(0).get("distance")) < 1.0);
        Assertions.assertTrue(((Double) result.get(1).get("distance")) > 10.0);
        assertNotNull(result.get(0).get("name"));
    }

    @Test
    void getCrowdForecast_exposesCurrentSnapshotWithoutFabricatingForecast() {
        AttractionRealtimeStatus status = new AttractionRealtimeStatus();
        status.setAttractionId(5L);
        status.setCrowdCount(320);
        status.setCrowdLevel(2);
        status.setUpdateTime(LocalDateTime.of(2026, 8, 18, 12, 0));
        when(realtimeStatusMapper.selectByAttractionId(5L)).thenReturn(status);

        Map<String, Object> result = service.getCrowdForecast(5, "2026-08-18");

        assertEquals(true, result.get("dataAvailable"));
        assertEquals(false, result.get("forecastAvailable"));
        assertEquals(2, result.get("overallLevel"));
        assertEquals(320, result.get("currentVisitorCount"));
        assertEquals(List.of(), result.get("hourlyForecast"));
    }

    @Test
    void getHistoricalCrowdData_reportsUnavailableInsteadOfRandomValues() {
        Map<String, Object> result = service.getHistoricalCrowdData(5, 30);

        assertEquals(false, result.get("dataAvailable"));
        assertNull(result.get("averageDailyVisitors"));
        assertEquals(List.of(), result.get("monthlyTrend"));
    }

    @Test
    void attractionMetadata_doesNotClaimUnverifiedFacilitiesOrAccessibility() {
        Attraction attraction = attraction(5, 10, "测试景点", 39.9, 116.4);
        when(attractionService.getById(5)).thenReturn(attraction);

        Map<String, Object> openingHours = service.getOpeningHoursDetail(5);
        Map<String, Object> accessibility = service.getAccessibilityInfo(5);

        assertEquals(false, openingHours.get("dataAvailable"));
        assertNull(openingHours.get("isOpenNow"));
        assertEquals(List.of(), service.getAttractionFacilities(5));
        assertEquals(false, accessibility.get("dataAvailable"));
        assertNull(accessibility.get("wheelchairAccessible"));
        assertEquals(List.of(), service.getPhotoSpots(5));
    }

    @Test
    void getRatingStatistics_aggregatesPersistedReviews() {
        when(attractionService.getById(5)).thenReturn(new Attraction());
        when(reviewMapper.selectRatingCounts(5)).thenReturn(List.of(
                Map.of("rating", 5, "rating_count", 3L),
                Map.of("rating", 3, "rating_count", 1L)));

        Map<String, Object> result = service.getRatingStatistics(5L);

        assertEquals(4.5, result.get("avgRating"));
        assertEquals(4L, result.get("totalReviews"));
        @SuppressWarnings("unchecked")
        Map<String, Long> distribution = (Map<String, Long>) result.get("ratingDistribution");
        assertEquals(3L, distribution.get("5"));
        assertEquals(1L, distribution.get("3"));
        assertEquals(0L, distribution.get("1"));
    }

    @Test
    void incrementViews_delegatesToAtomicUpdate() {
        when(attractionService.incrementViewCount(5)).thenReturn(true);

        assertTrue(service.incrementViews(5L));

        verify(attractionService).incrementViewCount(5);
        verify(attractionService, never()).updateById(any(Attraction.class));
    }
}
