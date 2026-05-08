package travel.service.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_recommendation.Restaurant;

import java.util.List;
import java.util.Map;

public interface RestaurantService extends IService<Restaurant> {

    /**
     * 根据城市ID查询餐厅列表
     */
    List<Restaurant> getByCityId(Integer cityId);

    /**
     * 根据菜系类型查询餐厅
     */
    List<Restaurant> getByCuisineType(Integer cityId, String cuisineType);

    /**
     * 根据价格等级查询餐厅
     */
    List<Restaurant> getByPriceLevel(Integer cityId, String priceLevel);

    /**
     * 获取高分餐厅
     */
    List<Restaurant> getTopRated(Integer cityId, int limit);

    /**
     * 根据距离获取餐厅
     */
    List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit);

    /**
     * 搜索餐厅
     */
    List<Restaurant> search(Integer cityId, String keyword);

    /**
     * 获取餐厅详情
     */
    Map<String, Object> getRestaurantDetail(Integer id);

    /**
     * 推荐餐厅
     */
    List<Map<String, Object>> recommendRestaurants(Integer cityId, Map<String, Object> preferences, int limit);
}
