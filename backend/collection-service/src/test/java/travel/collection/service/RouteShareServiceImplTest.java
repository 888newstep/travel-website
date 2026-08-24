package travel.collection.service;

import org.junit.jupiter.api.Test;
import travel.collection.service.impl.RouteShareServiceImpl;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.common.entity.user_community.RouteShare;
import travel.common.exception.BusinessException;
import travel.collection.dto.SharedRouteAccessResponse;
import travel.collection.dto.ShareStatisticsResponse;
import travel.common.utils.CacheUtil;
import travel.common.mapper.user_community_mapper.TravelNoteMapper;
import travel.common.mapper.travel_recommendation_mapper.ResourceFileMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

class RouteShareServiceImplTest {

    @Test
    void shouldRejectSharingAnotherUsersRoute() {
        RouteService routeService = mock(RouteService.class);
        Route route = new Route();
        route.setId(8);
        route.setUserId(7);
        when(routeService.getById(8L)).thenReturn(route);
        RouteShareServiceImpl service = new RouteShareServiceImpl(
                routeService, mock(UserService.class), mock(CacheUtil.class), mock(TravelNoteMapper.class),
                mock(ResourceFileMapper.class));

        RouteShare share = new RouteShare();
        share.setItemId(8);
        share.setRouteId(8);
        share.setItemType("route");
        share.setUserId(42);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.generateShareCode(share));

        assertEquals(2002, exception.getCode());
    }

    @Test
    void shouldRejectSharingAnotherUsersFile() {
        ResourceFileMapper resourceFileMapper = mock(ResourceFileMapper.class);
        ResourceFile file = new ResourceFile();
        file.setId(8);
        file.setUploadUserId(7);
        when(resourceFileMapper.selectById(8)).thenReturn(file);
        RouteShareServiceImpl service = new RouteShareServiceImpl(
                mock(RouteService.class), mock(UserService.class), mock(CacheUtil.class),
                mock(TravelNoteMapper.class), resourceFileMapper);

        RouteShare share = fileShare(8, 42);
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.generateShareCode(share));

        assertEquals(9009, exception.getCode());
    }

    @Test
    void shouldGenerateShareForOwnedFile() {
        ResourceFileMapper resourceFileMapper = mock(ResourceFileMapper.class);
        ResourceFile file = new ResourceFile();
        file.setId(8);
        file.setUploadUserId(42);
        file.setFileName("photo.jpg");
        when(resourceFileMapper.selectById(8)).thenReturn(file);
        RouteShareServiceImpl service = spy(new RouteShareServiceImpl(
                mock(RouteService.class), mock(UserService.class), mock(CacheUtil.class),
                mock(TravelNoteMapper.class), resourceFileMapper));
        doReturn(0L).when(service).count(any());
        doReturn(true).when(service).save(any(RouteShare.class));

        RouteShare result = service.generateShareCode(fileShare(8, 42));

        assertEquals("photo.jpg", result.getFileName());
        assertNotNull(result.getShareCode());
    }

    @Test
    void shouldReturnTypedRouteAccessAndIncrementVisitCount() {
        RouteShareServiceImpl service = spy(createService());
        RouteShare share = activeShare();
        doReturn(share).when(service).getByShareCode("VALID");
        doReturn(true).when(service).incrementVisitCount(8);

        SharedRouteAccessResponse result = service.accessShareRoute("VALID");

        assertEquals(9, result.routeId());
        assertEquals("Weekend", result.shareTitle());
    }

    @Test
    void shouldRejectMissingAndExpiredRouteShares() {
        RouteShareServiceImpl service = spy(createService());
        doReturn(null).when(service).getByShareCode("MISSING");
        BusinessException missing = assertThrows(
                BusinessException.class, () -> service.accessShareRoute("MISSING"));

        RouteShare expiredShare = activeShare();
        expiredShare.setExpireTime(LocalDateTime.now().minusMinutes(1));
        doReturn(expiredShare).when(service).getByShareCode("EXPIRED");
        BusinessException expired = assertThrows(
                BusinessException.class, () -> service.accessShareRoute("EXPIRED"));

        assertEquals(6001, missing.getCode());
        assertEquals(6002, expired.getCode());
    }

    @Test
    void shouldReturnTypedShareStatistics() {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        RouteShareServiceImpl service = spy(new RouteShareServiceImpl(
                mock(RouteService.class), mock(UserService.class), cacheUtil,
                mock(TravelNoteMapper.class), mock(ResourceFileMapper.class)));
        RouteShare share = activeShare();
        share.setUserId(42);
        share.setShareCode("VALID");
        share.setShareCount(2);
        share.setVisitCount(7);
        doReturn(share).when(service).getById(8L);

        ShareStatisticsResponse result = service.getShareStatistics(8L, 42);

        assertEquals(8L, result.shareId());
        assertEquals(7, result.visitCount());
        assertEquals(false, result.isExpired());
    }

    private RouteShareServiceImpl createService() {
        return new RouteShareServiceImpl(
                mock(RouteService.class), mock(UserService.class), mock(CacheUtil.class),
                mock(TravelNoteMapper.class), mock(ResourceFileMapper.class));
    }

    private RouteShare activeShare() {
        RouteShare share = new RouteShare();
        share.setId(8);
        share.setRouteId(9);
        share.setShareTitle("Weekend");
        share.setShareDescription("Two days");
        share.setIsActive(true);
        share.setExpireTime(LocalDateTime.now().plusDays(1));
        return share;
    }

    private RouteShare fileShare(Integer fileId, Integer userId) {
        RouteShare share = new RouteShare();
        share.setItemId(fileId);
        share.setItemType("file");
        share.setUserId(userId);
        return share;
    }
}
