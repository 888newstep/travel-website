package travel.collection.service;

import travel.collection.dto.UserStatisticsResponse;

public interface UserStatisticsService {

    /**
     * 获取用户统计信息
     */
    UserStatisticsResponse getCurrentUserStats();
}
