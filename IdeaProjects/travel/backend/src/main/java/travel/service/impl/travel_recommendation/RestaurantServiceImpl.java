package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.Restaurant;
import travel.mapper.travel_recommendation_mapper.RestaurantMapper;
import travel.service.travel_recommendation.RestaurantService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl extends ServiceImpl<RestaurantMapper, Restaurant> implements RestaurantService {

    @Override
    public List<Restaurant> getByCityId(Integer cityId) {
        try {
            QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取城市饭店列表失败: cityId={}", cityId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Restaurant> getByCuisineType(Integer cityId, String cuisineType) {
        try {
            QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.eq("cuisine_type", cuisineType);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取菜系饭店列表失败: cityId={}, cuisineType={}", cityId, cuisineType, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Restaurant> getByPriceLevel(Integer cityId, String priceLevel) {
        try {
            QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.eq("price_level", priceLevel);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取价格等级饭店列表失败: cityId={}, priceLevel={}", cityId, priceLevel, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Restaurant> getTopRated(Integer cityId, int limit) {
        try {
            QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.orderByDesc("rating");
            queryWrapper.last("LIMIT " + limit);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取高评分饭店列表失败: cityId={}, limit={}", cityId, limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit) {
        try {
            // 获取城市所有饭店
            List<Restaurant> restaurants = getByCityId(cityId);
            
            // 计算距离并排序
            List<Map<String, Object>> result = restaurants.stream()
                    .map(restaurant -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("restaurant", restaurant);
                        // 计算距离（这里使用简化的距离计算）
                        double distance = calculateDistance(latitude, longitude, restaurant.getLatitude(), restaurant.getLongitude());
                        map.put("distance", distance);
                        return map;
                    })
                    .sorted((m1, m2) -> Double.compare((Double) m1.get("distance"), (Double) m2.get("distance")))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            log.error("根据距离获取饭店列表失败: cityId={}, latitude={}, longitude={}, limit={}", cityId, latitude, longitude, limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Restaurant> search(Integer cityId, String keyword) {
        try {
            QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.like("name", keyword).or().like("feature", keyword).or().like("cuisine_type", keyword);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("搜索饭店失败: cityId={}, keyword={}", cityId, keyword, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getRestaurantDetail(Integer id) {
        try {
            Restaurant restaurant = getById(id);
            if (restaurant == null) {
                throw new RuntimeException("饭店不存在");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("restaurant", restaurant);
            // 这里可以添加更多信息，如评论、推荐菜等
            
            return result;
        } catch (Exception e) {
            log.error("获取饭店详情失败: id={}", id, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> recommendRestaurants(Integer cityId, Map<String, Object> preferences, int limit) {
        try {
            // 获取城市所有饭店
            List<Restaurant> restaurants = getByCityId(cityId);
            
            // 根据偏好筛选和排序
            List<Map<String, Object>> result = restaurants.stream()
                    .map(restaurant -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("restaurant", restaurant);
                        // 计算推荐分数
                        double score = calculateRecommendationScore(restaurant, preferences);
                        map.put("score", score);
                        return map;
                    })
                    .sorted((m1, m2) -> Double.compare((Double) m2.get("score"), (Double) m1.get("score")))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            log.error("推荐饭店失败: cityId={}, preferences={}, limit={}", cityId, preferences, limit, e);
            return new ArrayList<>();
        }
    }

    // 计算两点之间的距离（简化版）
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 这里使用简化的距离计算，实际项目中可以使用更精确的算法
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        return Math.sqrt(dLat * dLat + dLon * dLon) * 111; // 转换为公里
    }

    // 计算推荐分数
    private double calculateRecommendationScore(Restaurant restaurant, Map<String, Object> preferences) {
        double score = 0.0;
        
        // 基础分数：评分
        score += restaurant.getRating() * 20;
        
        // 根据偏好调整分数
        if (preferences != null) {
            // 菜系偏好
            if (preferences.containsKey("cuisineType")) {
                String preferredCuisine = (String) preferences.get("cuisineType");
                if (preferredCuisine.equals(restaurant.getCuisineType())) {
                    score += 10;
                }
            }
            
            // 价格偏好
            if (preferences.containsKey("priceLevel")) {
                String preferredPrice = (String) preferences.get("priceLevel");
                if (preferredPrice.equals(restaurant.getPriceLevel())) {
                    score += 10;
                }
            }
        }
        
        return score;
    }
}
