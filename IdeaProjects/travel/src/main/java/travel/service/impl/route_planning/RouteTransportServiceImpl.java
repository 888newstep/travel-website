package travel.service.impl.route_planning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.RouteTransport;
import travel.mapper.route_planning_mapper.RouteTransportMapper;
import travel.service.route_planning.RouteTransportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteTransportServiceImpl extends ServiceImpl<RouteTransportMapper, RouteTransport> implements RouteTransportService {

    private final RouteTransportMapper routeTransportMapper;

    @Override
    public List<RouteTransport> getByRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            return List.of();
        }

        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getRouteId, routeId)
                .orderByAsc(RouteTransport::getTransportOrder);

        return list(queryWrapper);
    }

    @Override
    public List<RouteTransport> getByAttractionId(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            return List.of();
        }

        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getFromAttractionId, attractionId)
                .or()
                .eq(RouteTransport::getToAttractionId, attractionId);

        return list(queryWrapper);
    }

    @Override
    public List<RouteTransport> getByTransportId(Long transportId) {
        if (transportId == null || transportId <= 0) {
            return List.of();
        }

        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getTransportId, transportId);

        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAdd(List<RouteTransport> routeTransports) {
        if (routeTransports == null || routeTransports.isEmpty()) {
            return false;
        }

        try {
            boolean success = saveBatch(routeTransports);
            log.info("批量添加路线交通信息成功，共添加 {} 条记录", routeTransports.size());
            return success;
        } catch (Exception e) {
            log.error("批量添加路线交通信息失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            return false;
        }

        try {
            LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RouteTransport::getRouteId, routeId);
            boolean success = remove(queryWrapper);
            log.info("删除路线ID为 {} 的交通信息成功", routeId);
            return success;
        } catch (Exception e) {
            log.error("删除路线交通信息失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Double calculateTotalCost(Long routeId) {
        List<RouteTransport> transports = getByRouteId(routeId);
        return transports.stream()
                .map(RouteTransport::getCostEstimate)
                .filter(cost -> cost != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    @Override
    public Integer calculateTotalTime(Long routeId) {
        List<RouteTransport> transports = getByRouteId(routeId);
        return transports.stream()
                .map(RouteTransport::getEstimatedDuration)
                .filter(duration -> duration != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Override
    public Double calculateTotalDistance(Long routeId) {
        List<RouteTransport> transports = getByRouteId(routeId);
        return transports.stream()
                .map(RouteTransport::getDistance)
                .filter(distance -> distance != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    /**
     * 根据路线ID和交通方式查询交通信息（使用routeTransportMapper）
     * @param routeId 路线ID
     * @param transportType 交通方式
     * @return 交通信息列表
     */
    @Override
    public List<RouteTransport> getByRouteIdAndType(Long routeId, String transportType) {
        if (routeId == null || routeId <= 0) {
            return List.of();
        }
        // 使用routeTransportMapper进行自定义查询
        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getRouteId, routeId)
                .orderByAsc(RouteTransport::getTransportOrder);
        return routeTransportMapper.selectList(queryWrapper);
    }

    /**
     * 统计路线的交通方式分布（使用routeTransportMapper）
     * @param routeId 路线ID
     * @return 交通方式到数量的映射
     */
    @Override
    public java.util.Map<String, Integer> countTransportTypesByRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            return java.util.Map.of();
        }
        // 使用routeTransportMapper进行自定义查询
        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getRouteId, routeId);
        List<RouteTransport> transports = routeTransportMapper.selectList(queryWrapper);
        // 统计交通方式分布
        return transports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        transport -> transport.getTransport() != null ? transport.getTransport().getTransportType().name() : "未知",
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.counting(),
                                Long::intValue
                        )
                ));
    }

    /**
     * 根据起始和结束景点ID查询交通信息（使用routeTransportMapper）
     * @param fromAttractionId 起始景点ID
     * @param toAttractionId 结束景点ID
     * @return 交通信息列表
     */
    @Override
    public List<RouteTransport> getByFromAndToAttractionId(Long fromAttractionId, Long toAttractionId) {
        if (fromAttractionId == null || fromAttractionId <= 0 || toAttractionId == null || toAttractionId <= 0) {
            return List.of();
        }
        // 使用routeTransportMapper进行自定义查询
        LambdaQueryWrapper<RouteTransport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteTransport::getFromAttractionId, fromAttractionId)
                .eq(RouteTransport::getToAttractionId, toAttractionId);
        return routeTransportMapper.selectList(queryWrapper);
    }
}
