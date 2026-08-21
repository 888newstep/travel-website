package travel.collection.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.collection.service.RouteCollectionService;
import travel.collection.service.RouteShareService;
import travel.collection.service.TravelNoteService;
import travel.collection.service.UserStatisticsService;
import travel.common.security.AuthenticatedUserSupport;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final TravelNoteService travelNoteService;
    private final RouteCollectionService routeCollectionService;
    private final RouteShareService routeShareService;

    @Override
    public Map<String, Object> getCurrentUserStats() {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        Map<String, Object> stats = new HashMap<>();

        try {
            int notesCount = travelNoteService.countByUserId(userId);
            int collectionsCount = (int) routeCollectionService.countByUserId(userId);
            int sharesCount = (int) routeShareService.countByUserId(userId);

            stats.put("notes", notesCount);
            stats.put("collections", collectionsCount);
            stats.put("shares", sharesCount);

            log.info("获取用户统计成功: userId={}, notes={}, collections={}, shares={}",
                    userId, notesCount, collectionsCount, sharesCount);
        } catch (Exception e) {
            log.error("获取用户统计失败: userId={}, error={}", userId, e.getMessage());
            stats.put("notes", 0);
            stats.put("collections", 0);
            stats.put("shares", 0);
        }

        return stats;
    }
}
