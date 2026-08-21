package travel.route.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.AMapRouteService;
import travel.common.service.DistributedLockService;
import travel.route.algorithm.GeneticAlgorithmTSP;
import travel.route.algorithm.RoutePlanAlgorithm;
import travel.route.dto.optimization.ApplyOptimizationRequest;
import travel.route.service.impl.RouteOptimizationServiceImpl;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceImplTest {

    @Mock
    private RouteService routeService;

    @Mock
    private AttractionService attractionService;

    @Mock
    private RouteAttractionService routeAttractionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private AMapRouteService aMapRouteService;

    @Mock
    private GeneticAlgorithmTSP geneticAlgorithmTSP;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RouteOptimizationServiceImpl routeOptimizationService;

    @Test
    void shouldApplyExplicitOrderOnlyOnceWhenRequestIsRepeated() {
        Route route = new Route();
        route.setId(31);
        RouteAttraction first = relation(1, 1);
        RouteAttraction second = relation(2, 2);
        RouteAttraction third = relation(3, 3);
        List<RouteAttraction> relations = List.of(first, second, third);
        ListOperations<String, Object> listOperations = mock(ListOperations.class);

        when(routeService.getById(31)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(31L)).thenReturn(relations);
        when(routeAttractionService.replaceRouteSchedule(eq(31), anyList())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        executeLockAndTransactionCallbacks();

        ApplyOptimizationRequest request = optimizationRequest(31, List.of(1, 3, 2));

        assertTrue(routeOptimizationService.applyOptimization(request));
        assertTrue(routeOptimizationService.applyOptimization(request));
        assertEquals(1, first.getVisitOrder());
        assertEquals(3, second.getVisitOrder());
        assertEquals(2, third.getVisitOrder());
        ArgumentCaptor<List<RouteAttraction>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(routeAttractionService, times(1)).replaceRouteSchedule(eq(31), scheduleCaptor.capture());
        assertEquals(3, scheduleCaptor.getValue().size());
        verify(listOperations, times(1)).leftPush(anyString(), any());
    }

    @Test
    void shouldRejectOrderThatDoesNotMatchRouteAttractions() {
        Route route = new Route();
        route.setId(32);
        when(routeService.getById(32)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(32L))
                .thenReturn(List.of(relation(1, 1), relation(2, 2)));
        executeLockAndTransactionCallbacks();

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.applyOptimization(
                        optimizationRequest(32, List.of(1, 99))));

        assertEquals(17003, exception.getCode());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldRejectOversizedRouteBeforeOptimizationWork() {
        Route route = new Route();
        route.setId(33);
        List<RouteAttraction> relations = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> relation(index, index))
                .toList();
        when(routeService.getById(33)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(33L)).thenReturn(relations);
        executeLockAndTransactionCallbacks();

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.applyOptimization(
                        optimizationRequest(33, IntStream.rangeClosed(1, 101).boxed().toList())));

        assertEquals(17003, exception.getCode());
        verifyNoInteractions(attractionService, redisTemplate);
    }

    @Test
    void shouldKeepCommittedOptimizationWhenHistoryCacheFails() {
        Route route = new Route();
        route.setId(34);
        List<RouteAttraction> relations = List.of(relation(1, 1), relation(2, 2));
        ListOperations<String, Object> listOperations = mock(ListOperations.class);

        when(routeService.getById(34)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(34L)).thenReturn(relations);
        when(routeAttractionService.replaceRouteSchedule(eq(34), anyList())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPush(anyString(), any())).thenThrow(new RuntimeException("redis unavailable"));
        executeLockAndTransactionCallbacks();

        assertTrue(routeOptimizationService.applyOptimization(optimizationRequest(34, List.of(2, 1))));
        verify(routeAttractionService).replaceRouteSchedule(eq(34), anyList());
    }

    @Test
    void shouldRejectAutomaticOptimizationWhenCoordinatesAreMissing() {
        Route route = new Route();
        route.setId(35);
        List<RouteAttraction> relations = List.of(relation(1, 1), relation(2, 2), relation(3, 3));
        Attraction incompleteAttraction = new Attraction();
        incompleteAttraction.setId(1);

        when(routeService.getById(35)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(35L)).thenReturn(relations);
        when(attractionService.getById(1)).thenReturn(incompleteAttraction);
        executeLockAndTransactionCallbacks();

        ApplyOptimizationRequest request = new ApplyOptimizationRequest();
        request.setRouteId(35);
        request.setOptimizationType("distance");

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.applyOptimization(request));

        assertEquals(17002, exception.getCode());
        verify(routeAttractionService, times(0)).replaceRouteSchedule(any(), anyList());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldFailWhenTransactionReturnsNoResult() {
        when(distributedLockService.executeWithLock(
                anyString(), org.mockito.ArgumentMatchers.<Supplier<Boolean>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<Boolean>>getArgument(1).get());
        when(transactionTemplate.execute(
                org.mockito.ArgumentMatchers.<TransactionCallback<Boolean>>any()))
                .thenReturn(null);

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.applyOptimization(
                        optimizationRequest(36, List.of(1, 2))));

        assertEquals(17001, exception.getCode());
        verifyNoInteractions(routeService, routeAttractionService, redisTemplate);
    }

    @Test
    void shouldRejectUnsupportedOptimizationTypeBeforeLocking() {
        ApplyOptimizationRequest request = new ApplyOptimizationRequest();
        request.setRouteId(37);
        request.setOptimizationType("time");

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.applyOptimization(request));

        assertEquals(17003, exception.getCode());
        verifyNoInteractions(distributedLockService, transactionTemplate, routeService);
    }

    @Test
    void shouldAdvertiseOnlyDistanceOptimization() {
        Route route = new Route();
        route.setId(38);
        when(routeService.getById(38)).thenReturn(route);

        List<travel.route.dto.optimization.OptimizationSuggestion> suggestions =
                routeOptimizationService.getOptimizationSuggestions(38);

        assertEquals(1, suggestions.size());
        assertEquals("distance", suggestions.get(0).getType());
    }

    @Test
    void shouldUseAmapTotalsOnceForFinalOptimizedOrder() {
        Attraction first = attraction(501, 120.0, 30.0, 20);
        Attraction second = attraction(502, 121.0, 31.0, null);
        AMapRouteService.RouteInfo routeInfo = new AMapRouteService.RouteInfo();
        routeInfo.setDistance(18.5);
        routeInfo.setDuration(42.0);
        routeInfo.setCost(6.0);

        when(attractionService.getById(501)).thenReturn(first);
        when(attractionService.getById(502)).thenReturn(second);
        when(geneticAlgorithmTSP.optimizeRoute(anyList(), eq("balanced")))
                .thenReturn(List.of(first, second));
        when(aMapRouteService.calculateMultiPointRoute(anyList())).thenReturn(routeInfo);

        RoutePlanAlgorithm.OptimalRoute result = routeOptimizationService.planOptimalRoute(
                List.of(501, 502), 1, BigDecimal.valueOf(500), "balanced");

        assertEquals(18.5, result.getTotalDistance());
        assertEquals(42.0, result.getTotalTime());
        assertEquals(26.0, result.getTotalCost());
        assertEquals(null, result.getDayPlans().get(0).getPoints().get(1).getDistance());
        verify(aMapRouteService, times(1)).calculateMultiPointRoute(anyList());
    }

    @Test
    void shouldFailPlanningWhenAmapTrafficIsUnavailable() {
        Attraction first = attraction(511, 120.0, 30.0, 10);
        Attraction second = attraction(512, 121.0, 31.0, 10);
        when(attractionService.getById(511)).thenReturn(first);
        when(attractionService.getById(512)).thenReturn(second);
        when(geneticAlgorithmTSP.optimizeRoute(anyList(), eq("fast")))
                .thenReturn(List.of(first, second));
        when(aMapRouteService.calculateMultiPointRoute(anyList())).thenReturn(null);

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.planOptimalRoute(
                        List.of(511, 512), 1, BigDecimal.valueOf(500), "fast"));

        assertEquals(20001, exception.getCode());
    }

    @Test
    void shouldNormalizeQualityScoreAndIgnoreMissingRatings() {
        Route route = new Route();
        route.setId(40);
        route.setTitle("quality-route");
        route.setDurationDays(1);
        List<RouteAttraction> relations = IntStream.rangeClosed(1, 4)
                .mapToObj(index -> relation(400 + index, index))
                .toList();

        when(routeService.getById(40)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(40L)).thenReturn(relations);
        for (int index = 1; index <= 4; index++) {
            Attraction attraction = new Attraction();
            attraction.setId(400 + index);
            attraction.setDescription("历史文化");
            attraction.setRating(index == 4 ? null : BigDecimal.valueOf(5));
            when(attractionService.getById(400 + index)).thenReturn(attraction);
        }

        travel.route.dto.optimization.RouteQualityEvaluationResult result =
                routeOptimizationService.evaluateRouteQuality(40);

        assertEquals(5.0, result.getAverageRating());
        assertEquals(4.0, result.getAttractionsPerDay());
        assertEquals(0.76, result.getQualityScore(), 0.0001);
        assertEquals("推荐", result.getRecommendationLevel());
    }

    @Test
    void shouldRejectQualityEvaluationForInvalidDuration() {
        Route route = new Route();
        route.setId(41);
        route.setDurationDays(0);
        when(routeService.getById(41)).thenReturn(route);

        travel.common.exception.BusinessException exception = assertThrows(
                travel.common.exception.BusinessException.class,
                () -> routeOptimizationService.evaluateRouteQuality(41));

        assertEquals(2016, exception.getCode());
        verifyNoInteractions(routeAttractionService);
    }

    private void executeLockAndTransactionCallbacks() {
        when(distributedLockService.executeWithLock(
                anyString(), org.mockito.ArgumentMatchers.<Supplier<Boolean>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<Boolean>>getArgument(1).get());
        when(transactionTemplate.execute(
                org.mockito.ArgumentMatchers.<TransactionCallback<Boolean>>any()))
                .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0)
                        .doInTransaction(null));
    }

    private ApplyOptimizationRequest optimizationRequest(Integer routeId, List<Integer> attractionOrder) {
        ApplyOptimizationRequest request = new ApplyOptimizationRequest();
        request.setRouteId(routeId);
        request.setSuggestion(Map.of(
                "type", TextNode.valueOf("distance"),
                "attractionOrder", new ObjectMapper().valueToTree(attractionOrder)));
        return request;
    }

    private RouteAttraction relation(Integer attractionId, Integer visitOrder) {
        RouteAttraction relation = new RouteAttraction();
        relation.setId(attractionId);
        relation.setRouteId(31);
        relation.setAttractionId(attractionId);
        relation.setDayNumber(1);
        relation.setVisitOrder(visitOrder);
        return relation;
    }

    private Attraction attraction(Integer id, Double longitude, Double latitude, Integer ticketPrice) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setLongitude(BigDecimal.valueOf(longitude));
        attraction.setLatitude(BigDecimal.valueOf(latitude));
        attraction.setTicketPrice(ticketPrice == null ? null : BigDecimal.valueOf(ticketPrice));
        return attraction;
    }
}
