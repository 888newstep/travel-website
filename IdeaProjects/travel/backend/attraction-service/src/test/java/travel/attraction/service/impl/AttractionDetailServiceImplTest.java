package travel.attraction.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_recommendation.AttractionReview;
import travel.common.mapper.travel_recommendation_mapper.AttractionReviewMapper;
import travel.attraction.service.AttractionService;
import travel.common.utils.CacheUtil;

import java.math.BigDecimal;
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
    private AttractionDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        attractionService = mock(AttractionService.class);
        cacheUtil = mock(CacheUtil.class);
        reviewMapper = mock(AttractionReviewMapper.class);
        service = new AttractionDetailServiceImpl(attractionService, cacheUtil, reviewMapper);
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

        when(reviewMapper.selectList(any())).thenReturn(Arrays.asList(review));

        List<Map<String, Object>> result = service.getAttractionReviews(5L, 0, 10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).get("id"));
        assertEquals(4, result.get(0).get("rating"));
        assertEquals("不错", result.get(0).get("content"));
        assertNotNull(result.get(0).get("createTime"));
        verify(reviewMapper, times(1)).selectList(any());
    }

    @Test
    void saveAttractionReview_insertsAndReturnsReview() {
        when(reviewMapper.insert(any(AttractionReview.class))).thenAnswer(inv -> {
            AttractionReview r = inv.getArgument(0);
            r.setId(99);  // 模拟自增回填
            return 1;
        });

        Map<String, Object> result = service.saveAttractionReview(7, 2, 5, "很好");

        assertEquals(99, result.get("id"));
        assertEquals(7, result.get("attractionId"));
        assertEquals(2, result.get("userId"));
        assertEquals(5, result.get("rating"));
        assertNotNull(result.get("createTime"));
        verify(reviewMapper, times(1)).insert(any(AttractionReview.class));
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
}