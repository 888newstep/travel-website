package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.route_planning.Transport;
import travel.mapper.route_planning_mapper.RouteMapper;
import travel.mapper.route_planning_mapper.RouteAttractionMapper;
import travel.mapper.route_planning_mapper.TransportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 路线规划Repository接口
 */
@Repository
public class RoutePlanRepository {

    @Autowired
    private RouteMapper routeMapper;

    @Autowired
    private RouteAttractionMapper routeAttractionMapper;

    @Autowired
    private TransportMapper transportMapper;

    /**
     * 根据城市ID和天数规划路线
     */
    public List<Route> findRoutesByCityAndDays(Long cityId, Integer days) {
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        if (days != null) {
            queryWrapper.eq("days", days);
        }
        return routeMapper.selectList(queryWrapper);
    }

    /**
     * 根据路线ID获取路线景点
     */
    public List<Integer> findAttractionIdsByRouteId(Long routeId) {
        QueryWrapper<RouteAttraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route_id", routeId);
        queryWrapper.orderByAsc("sort_order");
        return routeAttractionMapper.selectList(queryWrapper)
                .stream()
                .map(RouteAttraction::getAttractionId)
                .collect(Collectors.toList());
    }

    /**
     * 根据城市ID获取可用的交通方式
     */
    public List<Transport> findAvailableTransportsByCityId(Long cityId) {
        QueryWrapper<Transport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        queryWrapper.eq("status", "available");
        return transportMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户偏好推荐路线
     */
    public List<Route> recommendRoutesByPreferences(Long cityId, List<String> preferences) {
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        // 这里可以根据实际的偏好字段进行查询
        // 例如：queryWrapper.in("theme", preferences);
        return routeMapper.selectList(queryWrapper);
    }
}
