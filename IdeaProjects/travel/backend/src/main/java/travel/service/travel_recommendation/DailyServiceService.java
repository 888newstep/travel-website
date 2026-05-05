package travel.service.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_recommendation.DailyService;

import java.util.List;
import java.util.Map;

public interface DailyServiceService extends IService<DailyService> {

    /**
     * 根据城市ID获取日常服务列表
     */
    List<DailyService> getByCityId(Integer cityId);

    /**
     * 根据服务类型获取日常服务列表
     */
    List<DailyService> getByServiceType(Integer cityId, String serviceType);

    /**
     * 获取高评分日常服务列表
     */
    List<DailyService> getTopRated(Integer cityId, int limit);

    /**
     * 根据距离排序获取日常服务列表
     */
    List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit);

    /**
     * 搜索日常服务
     */
    List<DailyService> search(Integer cityId, String keyword);

    /**
     * 获取日常服务详情
     */
    Map<String, Object> getServiceDetail(Integer id);

    /**
     * 推荐日常服务
     */
    List<Map<String, Object>> recommendServices(Integer cityId, Map<String, Object> preferences, int limit);
}
