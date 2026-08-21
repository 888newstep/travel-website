package travel.attraction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_recommendation.Restaurant;
import travel.common.mapper.travel_recommendation_mapper.RestaurantMapper;
import travel.attraction.service.RestaurantService;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl extends ServiceImpl<RestaurantMapper, Restaurant> implements RestaurantService {

    @Override
    public List<Restaurant> getByCityId(Integer cityId) {
        validateCityId(cityId);
        QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        queryWrapper.orderByDesc("rating");
        return list(queryWrapper);
    }

    @Override
    public List<Restaurant> getByCuisineType(Integer cityId, String cuisineType) {
        validateCityId(cityId);
        validateText(cuisineType);
        QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        queryWrapper.eq("cuisine_type", cuisineType.trim());
        queryWrapper.orderByDesc("rating");
        return list(queryWrapper);
    }

    @Override
    public List<Restaurant> getByPriceLevel(Integer cityId, String priceLevel) {
        validateCityId(cityId);
        validateText(priceLevel);
        QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        queryWrapper.eq("price_level", priceLevel.trim());
        queryWrapper.orderByDesc("rating");
        return list(queryWrapper);
    }

    @Override
    public List<Restaurant> getTopRated(Integer cityId, int limit) {
        validateCityId(cityId);
        validateLimit(limit);
        QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        queryWrapper.orderByDesc("rating");
        queryWrapper.last("LIMIT " + limit);
        return list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit) {
        validateCityId(cityId);
        validateLimit(limit);
        validateCoordinates(latitude, longitude);
        List<Restaurant> restaurants = getByCityId(cityId);

        return restaurants.stream()
                .filter(restaurant -> restaurant.getLatitude() != null && restaurant.getLongitude() != null)
                .map(restaurant -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("restaurant", restaurant);
                    double distance = calculateDistance(
                            latitude, longitude, restaurant.getLatitude(), restaurant.getLongitude());
                    map.put("distance", distance);
                    return map;
                })
                .sorted((m1, m2) -> Double.compare(
                        (Double) m1.get("distance"), (Double) m2.get("distance")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Restaurant> search(Integer cityId, String keyword) {
        validateCityId(cityId);
        validateText(keyword);
        String normalizedKeyword = keyword.trim();
        QueryWrapper<Restaurant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId)
                .and(wrapper -> wrapper.like("name", normalizedKeyword)
                        .or().like("feature", normalizedKeyword)
                        .or().like("cuisine_type", normalizedKeyword));
        queryWrapper.orderByDesc("rating");
        return list(queryWrapper);
    }

    @Override
    public Map<String, Object> getRestaurantDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Restaurant restaurant = getById(id);
        if (restaurant == null) {
            throw new BusinessException(ErrorCodeEnum.RESTAURANT_NOT_EXIST);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("restaurant", restaurant);
        return result;
    }

    @Override
    public List<Map<String, Object>> recommendRestaurants(Integer cityId, Map<String, Object> preferences, int limit) {
        validateCityId(cityId);
        validateLimit(limit);
        List<Restaurant> restaurants = getByCityId(cityId);

        return restaurants.stream()
                .map(restaurant -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("restaurant", restaurant);
                    map.put("score", calculateRecommendationScore(restaurant, preferences));
                    return map;
                })
                .sorted((m1, m2) -> Double.compare(
                        (Double) m2.get("score"), (Double) m1.get("score")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void validateCityId(Integer cityId) {
        if (cityId == null || cityId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_RANGE_ERROR);
        }
    }

    private void validateText(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new BusinessException(ErrorCodeEnum.PARAM_RANGE_ERROR);
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
