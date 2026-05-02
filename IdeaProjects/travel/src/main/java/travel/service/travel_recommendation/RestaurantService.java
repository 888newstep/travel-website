package travel.service.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_recommendation.Restaurant;

import java.util.List;
import java.util.Map;

public interface RestaurantService extends IService<Restaurant> {

    /**
     * 根据城市ID获取饭店列表
     */
    List<Restaurant> getByCityId(Integer cityId);

    /**
     * 根据菜系获取饭店列表
     */
    List<Restaurant> getByCuisineType(Integer cityId, String cuisineType);

    /**
     * 根据价格等级获取饭店列表
     */
    List<Restaurant> getByPriceLevel(Integer cityId, String priceLevel);

    /**
     * 获取高评分饭店列表
     */
    List<Restaurant> getTopRated(Integer cityId, int limit);

    /**
     * 根据距离排序获取饭店列表
     */
    List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit);

    /**
     * 搜索饭店
     */
    List<Restaurant> search(Integer cityId, String keyword);

    /**
     * 获取饭店详情
     */
    Map<String, Object> getRestaurantDetail(Integer id);

    /**
     * 推荐饭店
     */
    List<Map<String, Object>> recommendRestaurants(Integer cityId, Map<String, Object> preferences, int limit);
}
