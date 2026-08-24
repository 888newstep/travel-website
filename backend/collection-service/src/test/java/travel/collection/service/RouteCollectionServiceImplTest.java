package travel.collection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.collection.service.impl.RouteCollectionServiceImpl;
import travel.common.entity.route_planning.Route;
import travel.common.entity.user_community.RouteCollection;
import travel.common.entity.user_community.User;
import travel.common.performance.PerformanceStageRecorder;
import travel.common.service.DistributedLockService;
import travel.common.utils.CacheUtil;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteCollectionServiceImplTest {

    @Mock
    private RouteService routeService;
    @Mock
    private DistributedLockService distributedLockService;
    @Mock
    private UserService userService;
    @Mock
    private CacheUtil cacheUtil;

    private RouteCollectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new RouteCollectionServiceImpl(
                routeService,
                distributedLockService,
                userService,
                cacheUtil,
                PerformanceStageRecorder.disabled()));
        when(distributedLockService.executeWithLock(anyString(), org.mockito.ArgumentMatchers.<Supplier<Boolean>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<Boolean>>getArgument(1).get());
    }

    @Test
    void shouldCreateCollectionAndInvalidateCollectedStateWithinToggleLock() {
        Route route = new Route();
        route.setId(8);
        User user = new User();
        user.setId(42);

        doReturn(null).when(service).getOne(any(LambdaQueryWrapper.class), eq(false));
        doReturn(true).when(service).save(any(RouteCollection.class));
        when(routeService.getById(8L)).thenReturn(route);
        when(userService.getById(42L)).thenReturn(user);

        boolean collected = service.toggleCollection(8, 42);

        assertTrue(collected);
        verify(distributedLockService).executeWithLock(
                eq("collection:42:8"), org.mockito.ArgumentMatchers.<Supplier<Boolean>>any());
        verify(cacheUtil).delete(CacheUtil.generateKey(
                CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", 8, 42));
    }

    @Test
    void shouldRemoveExistingCollectionWithinSameToggleLock() {
        RouteCollection existing = new RouteCollection();
        existing.setId(9);
        existing.setRouteId(8);
        existing.setUserId(42);
        existing.setIsPublic(false);

        doReturn(existing).when(service).getOne(any(LambdaQueryWrapper.class), eq(false));
        doReturn(true).when(service).removeById(9);

        boolean collected = service.toggleCollection(8, 42);

        assertFalse(collected);
        verify(service).removeById(9);
        verify(routeService, never()).getById(any());
        verify(userService, never()).getById(any());
        verify(cacheUtil).delete(CacheUtil.generateKey(
                CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", 8, 42));
    }
}
