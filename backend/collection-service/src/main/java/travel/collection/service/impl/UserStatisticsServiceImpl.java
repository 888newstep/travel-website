package travel.collection.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.collection.service.RouteCollectionService;
import travel.collection.service.RouteShareService;
import travel.collection.service.TravelNoteService;
import travel.collection.service.UserStatisticsService;
import travel.collection.dto.UserStatisticsResponse;
import travel.common.security.AuthenticatedUserSupport;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final TravelNoteService travelNoteService;
    private final RouteCollectionService routeCollectionService;
    private final RouteShareService routeShareService;

    @Override
    public UserStatisticsResponse getCurrentUserStats() {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        int notesCount = travelNoteService.countByUserId(userId);
        int collectionsCount = Math.toIntExact(routeCollectionService.countByUserId(userId));
        int sharesCount = Math.toIntExact(routeShareService.countByUserId(userId));

        log.info("获取用户统计成功: userId={}, notes={}, collections={}, shares={}",
                userId, notesCount, collectionsCount, sharesCount);
        return new UserStatisticsResponse(notesCount, collectionsCount, sharesCount);
    }
}
