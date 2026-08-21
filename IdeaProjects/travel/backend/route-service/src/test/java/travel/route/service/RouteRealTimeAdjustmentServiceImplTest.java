package travel.route.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.mapper.route_planning_mapper.RouteAttractionMapper;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.AMapService;
import travel.route.service.impl.RouteRealTimeAdjustmentServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteRealTimeAdjustmentServiceImplTest {

    @Test
    void shouldBuildTrafficSegmentsFromAmapTmcStatus() throws Exception {
        RouteMapper routeMapper = mock(RouteMapper.class);
        RouteAttractionMapper routeAttractionMapper = mock(RouteAttractionMapper.class);
        AttractionMapper attractionMapper = mock(AttractionMapper.class);
        AttractionRealtimeStatusMapper realtimeStatusMapper = mock(AttractionRealtimeStatusMapper.class);
        AMapService aMapService = mock(AMapService.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(routeAttractionMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(relation(9, 8, 1), relation(9, 10, 2)));
        when(attractionMapper.selectLatLngById(8L)).thenReturn(attraction(8, 116.30, 39.90));
        when(attractionMapper.selectLatLngById(10L)).thenReturn(attraction(10, 116.40, 39.95));
        when(aMapService.drivingRoute(116.30, 39.90, 116.40, 39.95)).thenReturn(Map.of(
                "distance", 12000,
                "duration", 1800,
                "steps", new ObjectMapper().readTree("""
                        [{"tmcs":[{"status":"畅通"},{"status":"拥堵"}]}]
                        """)));

        RouteRealTimeAdjustmentServiceImpl service = new RouteRealTimeAdjustmentServiceImpl(
                routeMapper, redisTemplate, routeAttractionMapper, attractionMapper,
                realtimeStatusMapper, aMapService);

        Map<String, Object> result = service.getRealTimeTrafficInfo(9L);

        assertEquals(true, result.get("dataAvailable"));
        assertEquals("amap", result.get("source"));
        assertEquals("heavy", result.get("segment:8-10"));
        assertEquals(12000L, result.get("totalDistanceMeters"));
        assertEquals(1800L, result.get("totalDurationSeconds"));
    }

    private RouteAttraction relation(int routeId, int attractionId, int visitOrder) {
        RouteAttraction relation = new RouteAttraction();
        relation.setRouteId(routeId);
        relation.setAttractionId(attractionId);
        relation.setDayNumber(1);
        relation.setVisitOrder(visitOrder);
        return relation;
    }

    private Attraction attraction(int id, double longitude, double latitude) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setLongitude(BigDecimal.valueOf(longitude));
        attraction.setLatitude(BigDecimal.valueOf(latitude));
        return attraction;
    }
}
