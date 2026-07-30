package travel.collection.service;

import java.util.Map;

public interface UserStatisticsService {

    /**
     * 获取用户统计信息
     */
    Map<String, Object> getUserStats(Integer userId);
}
