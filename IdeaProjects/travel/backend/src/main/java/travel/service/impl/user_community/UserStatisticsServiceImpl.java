package travel.service.impl.user_community;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.service.user_community.RouteCollectionService;
import travel.service.user_community.RouteShareService;
import travel.service.user_community.TravelNoteService;
import travel.service.user_community.UserStatisticsService;
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
    public Map<String, Object> getUserStats(Integer userId) {
        if (userId == null || userId <= 0) {
            log.warn("无效的用户ID: {}", userId);
            return new HashMap<>();
        }

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
