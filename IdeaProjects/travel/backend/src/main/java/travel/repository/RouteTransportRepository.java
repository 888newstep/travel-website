package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import travel.entity.route_planning.RouteTransport;
import travel.mapper.route_planning_mapper.RouteTransportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 路线交通关联Repository接口
 */
@Repository
public class RouteTransportRepository {

    @Autowired
    private RouteTransportMapper routeTransportMapper;

    /**
     * 保存路线交通关联
     */
    public RouteTransport save(RouteTransport routeTransport) {
        routeTransportMapper.insert(routeTransport);
        return routeTransport;
    }

    /**
     * 批量保存路线交通关联
     */
    public boolean batchSave(List<RouteTransport> routeTransports) {
        if (routeTransports == null || routeTransports.isEmpty()) {
            return false;
        }
        for (RouteTransport routeTransport : routeTransports) {
            routeTransportMapper.insert(routeTransport);
        }
        return true;
    }

    /**
     * 更新路线交通关联
     */
    public boolean update(RouteTransport routeTransport) {
        return routeTransportMapper.updateById(routeTransport) > 0;
    }

    /**
     * 根据ID删除路线交通关联
     */
    public boolean deleteById(Long id) {
        return routeTransportMapper.deleteById(id) > 0;
    }

    /**
     * 根据路线ID删除路线交通关联
     */
    public boolean deleteByRouteId(Long routeId) {
        QueryWrapper<RouteTransport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route_id", routeId);
        return routeTransportMapper.delete(queryWrapper) > 0;
    }

    /**
     * 根据路线ID查询路线交通关联
     */
    public List<RouteTransport> findByRouteId(Long routeId) {
        QueryWrapper<RouteTransport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route_id", routeId);
        queryWrapper.orderByAsc("sort_order");
        return routeTransportMapper.selectList(queryWrapper);
    }

    /**
     * 根据交通ID查询路线交通关联
     */
    public List<RouteTransport> findByTransportId(Long transportId) {
        QueryWrapper<RouteTransport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("transport_id", transportId);
        return routeTransportMapper.selectList(queryWrapper);
    }
}
