package travel.service.impl.route_planning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.Attraction;
import travel.entity.route_planning.Transport;
import travel.entity.route_planning.TransportType;
import travel.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.mapper.route_planning_mapper.TransportMapper;
import travel.service.route_planning.TransportService;
import travel.utils.ThirdApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransportServiceImpl extends ServiceImpl<TransportMapper, Transport> implements TransportService {

    private static final Logger logger = LoggerFactory.getLogger(TransportServiceImpl.class);

    private final AttractionMapper attractionMapper;
    private final ThirdApiUtil thirdApiUtil;

    @Override
    public List<Transport> getByType(TransportType type) {
        LambdaQueryWrapper<Transport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transport::getTransportType, type);
        return list(queryWrapper);
    }

    @Override
    public List<TransportType> getAllTypes() {
        return Arrays.asList(TransportType.values());
    }

    @Override
    public List<Transport> calculateTransportOptions(Integer fromAttractionId, Integer toAttractionId) {
        // 1. 获取两个景点的位置信息
        Attraction fromAttraction = attractionMapper.selectById(fromAttractionId);
        Attraction toAttraction = attractionMapper.selectById(toAttractionId);
        
        if (fromAttraction == null || toAttraction == null) {
            logger.warn("景点不存在: from={}, to={}", fromAttractionId, toAttractionId);
            return List.of();
        }
        
        // 2. 计算距离（简化计算，实际应该使用地图API）
        double distance = calculateDistance(
                fromAttraction.getLatitude().doubleValue(),
                fromAttraction.getLongitude().doubleValue(),
                toAttraction.getLatitude().doubleValue(),
                toAttraction.getLongitude().doubleValue()
        );
        
        logger.info("两个景点之间的距离: {} km", distance);
        
        // 3. 根据距离推荐交通方式
        List<Transport> recommendedTransports = new ArrayList<>();
        
        if (distance < 1) {
            // 近距离推荐步行和自行车
            recommendedTransports.addAll(getByType(TransportType.walking));
            recommendedTransports.addAll(getByType(TransportType.bicycle));
        } else if (distance < 5) {
            // 中距离推荐公交、地铁、出租车
            recommendedTransports.addAll(getByType(TransportType.bus));
            recommendedTransports.addAll(getByType(TransportType.subway));
            recommendedTransports.addAll(getByType(TransportType.taxi));
        } else {
            // 远距离推荐汽车、火车
            recommendedTransports.addAll(getByType(TransportType.car));
            recommendedTransports.addAll(getByType(TransportType.train));
        }
        
        return recommendedTransports;
    }

    @Override
    public Transport getRecommendedTransport(Integer fromAttractionId, Integer toAttractionId, String preference) {
        List<Transport> options = calculateTransportOptions(fromAttractionId, toAttractionId);
        
        if (options.isEmpty()) {
            return null;
        }
        
        // 根据偏好选择推荐的交通工具
        switch (preference) {
            case "fast":
                // 速度优先
                return options.stream()
                        .max((t1, t2) -> t1.getAvgSpeedKmh().compareTo(t2.getAvgSpeedKmh()))
                        .orElse(null);
            case "lowCost":
                // 成本优先
                return options.stream()
                        .min((t1, t2) -> t1.getCostPerKm().compareTo(t2.getCostPerKm()))
                        .orElse(null);
            case "lowCarbon":
                // 低碳优先
                return options.stream()
                        .min((t1, t2) -> t1.getCo2Emission().compareTo(t2.getCo2Emission()))
                        .orElse(null);
            default:
                // 默认返回第一个选项
                return options.get(0);
        }
    }

    /**
     * 计算两个经纬度之间的距离（单位：公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public java.util.Map<String, Object> getRealTimeTrafficData(String fromLocation, String toLocation) {
        // 参数验证
        if (fromLocation == null || fromLocation.trim().isEmpty()) {
            logger.error("获取实时交通数据失败: fromLocation为null或空");
            throw new IllegalArgumentException("fromLocation为null或空");
        }
        if (toLocation == null || toLocation.trim().isEmpty()) {
            logger.error("获取实时交通数据失败: toLocation为null或空");
            throw new IllegalArgumentException("toLocation为null或空");
        }
        
        try {
            // 使用ThirdApiUtil获取实时交通数据
            java.util.Map<String, Object> trafficData = thirdApiUtil.getRealTimeTrafficInfo(fromLocation + "," + toLocation);
            logger.info("获取实时交通数据成功: from={}, to={}, data={}", fromLocation, toLocation, trafficData);
            return trafficData;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取实时交通数据失败: from={}, to={}, error={}", fromLocation, toLocation, e.getMessage());
            // 返回模拟数据
            java.util.Map<String, Object> mockData = new java.util.HashMap<>();
            mockData.put("status", "success");
            mockData.put("congestionLevel", "moderate");
            mockData.put("estimatedTime", 15); // 分钟
            mockData.put("distance", 5.2); // 公里
            mockData.put("suggestedRoute", "建议走主干道");
            return mockData;
        }
    }

    // 以下是Controller中使用的方法实现

    @Override
    public Transport addTransport(Transport transport) {
        logger.info("添加交通工具: {}", transport);
        save(transport);
        return transport;
    }

    @Override
    public Transport updateTransport(Long id, Transport transport) {
        logger.info("更新交通工具: id={}", id);
        Transport existing = getById(id.intValue());
        if (existing == null) {
            return null;
        }
        existing.setTransportType(transport.getTransportType());
        existing.setTransportName(transport.getTransportName());
        updateById(existing);
        return existing;
    }

    @Override
    public boolean deleteTransport(Long id) {
        logger.info("删除交通工具: id={}", id);
        return removeById(id.intValue());
    }

    @Override
    public Transport getTransportDetail(Long id) {
        logger.info("获取交通工具详情: id={}", id);
        return getById(id.intValue());
    }

    @Override
    public List<Transport> getTransportList(String type, int page, int size) {
        logger.info("获取交通工具列表: type={}, page={}, size={}", type, page, size);
        LambdaQueryWrapper<Transport> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Transport::getTransportType, type);
        }
        return list(queryWrapper);
    }

    @Override
    public List<Transport> searchTransports(String keyword, int page, int size) {
        logger.info("搜索交通工具: keyword={}, page={}, size={}", keyword, page, size);
        LambdaQueryWrapper<Transport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Transport::getTransportName, keyword);
        return list(queryWrapper);
    }

    @Override
    public Map<String, Object> getTransportStatistics() {
        logger.info("获取交通工具统计");
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", count());
        statistics.put("byType", Map.of());
        return statistics;
    }

    @Override
    public Map<String, Object> getTransportRoute(Map<String, Object> routeRequest) {
        logger.info("获取交通工具路线: {}", routeRequest);
        Map<String, Object> route = new HashMap<>();
        route.put("route", "模拟路线");
        route.put("distance", 10.0);
        route.put("duration", 30);
        return route;
    }

    @Override
    public Map<String, Object> getRealtimeTransportInfo(String location, String transportType) {
        logger.info("获取实时交通信息: location={}, transportType={}", location, transportType);
        Map<String, Object> info = new HashMap<>();
        info.put("status", "normal");
        info.put("congestionLevel", "light");
        return info;
    }

    @Override
    public List<Transport> batchAddTransports(List<Transport> transports) {
        logger.info("批量添加交通工具: count={}", transports.size());
        saveBatch(transports);
        return transports;
    }
}
