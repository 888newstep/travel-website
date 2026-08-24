package travel.route.service;

import java.util.List;
import java.util.Map;

/**
 * 路线实时调整服务接口
 */
public interface RouteRealTimeAdjustmentService {

    /**
     * 获取实时交通信息
     * @param routeId 路线ID
     * @return 实时交通信息
     */
    Map<String, Object> getRealTimeTrafficInfo(Long routeId);

    /**
     * 获取景点实时状态
     * @param attractionIds 景点ID列表
     * @return 景点实时状态
     */
    Map<Long, Map<String, Object>> getRealTimeAttractionStatus(List<Long> attractionIds);

}
