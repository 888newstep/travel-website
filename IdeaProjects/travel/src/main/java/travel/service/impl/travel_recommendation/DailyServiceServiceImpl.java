package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.DailyService;
import travel.mapper.travel_recommendation_mapper.DailyServiceMapper;
import travel.service.travel_recommendation.DailyServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyServiceServiceImpl extends ServiceImpl<DailyServiceMapper, DailyService> implements DailyServiceService {

    private static final Logger log = LoggerFactory.getLogger(DailyServiceServiceImpl.class);

    @Override
    public List<DailyService> getByCityId(Integer cityId) {
        try {
            QueryWrapper<DailyService> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取城市日常服务列表失败: cityId=" + cityId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<DailyService> getByServiceType(Integer cityId, String serviceType) {
        try {
            QueryWrapper<DailyService> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.eq("service_type", serviceType);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取服务类型日常服务列表失败: cityId=" + cityId + ", serviceType=" + serviceType, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<DailyService> getTopRated(Integer cityId, int limit) {
        try {
            QueryWrapper<DailyService> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.orderByDesc("rating");
            queryWrapper.last("LIMIT " + limit);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取高评分日常服务列表失败: cityId=" + cityId + ", limit=" + limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getByDistance(Integer cityId, Double latitude, Double longitude, int limit) {
        try {
            // 获取城市所有日常服务
            List<DailyService> services = getByCityId(cityId);
            
            // 计算距离并排序
            List<Map<String, Object>> result = services.stream()
                    .map(service -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("service", service);
                        // 计算距离（这里使用简化的距离计算）
                        double distance = calculateDistance(latitude, longitude, service.getLatitude(), service.getLongitude());
                        map.put("distance", distance);
                        return map;
                    })
                    .sorted((m1, m2) -> Double.compare((Double) m1.get("distance"), (Double) m2.get("distance")))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            log.error("根据距离获取日常服务列表失败: cityId=" + cityId + ", latitude=" + latitude + ", longitude=" + longitude + ", limit=" + limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<DailyService> search(Integer cityId, String keyword) {
        try {
            QueryWrapper<DailyService> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.like("name", keyword).or().like("feature", keyword).or().like("service_type", keyword);
            queryWrapper.orderByDesc("rating");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("搜索日常服务失败: cityId=" + cityId + ", keyword=" + keyword, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getServiceDetail(Integer id) {
        try {
            DailyService service = getById(id);
            if (service == null) {
                throw new RuntimeException("服务不存在");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("service", service);
            // 这里可以添加更多信息，如评论等
            
            return result;
        } catch (Exception e) {
            log.error("获取日常服务详情失败: id=" + id, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> recommendServices(Integer cityId, Map<String, Object> preferences, int limit) {
        try {
            // 获取城市所有日常服务
            List<DailyService> services = getByCityId(cityId);
            
            // 根据偏好筛选和排序
            List<Map<String, Object>> result = services.stream()
                    .map(service -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("service", service);
                        // 计算推荐分数
                        double score = calculateRecommendationScore(service, preferences);
                        map.put("score", score);
                        return map;
                    })
                    .sorted((m1, m2) -> Double.compare((Double) m2.get("score"), (Double) m1.get("score")))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            log.error("推荐日常服务失败: cityId=" + cityId + ", preferences=" + preferences + ", limit=" + limit, e);
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
    private double calculateRecommendationScore(DailyService service, Map<String, Object> preferences) {
        double score = 0.0;
        
        // 基础分数：评分
        score += service.getRating() * 20;
        
        // 根据偏好调整分数
        if (preferences != null) {
            // 服务类型偏好
            if (preferences.containsKey("serviceType")) {
                String preferredType = (String) preferences.get("serviceType");
                if (preferredType.equals(service.getServiceType())) {
                    score += 10;
                }
            }
        }
        
        return score;
    }
}
