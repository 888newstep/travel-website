package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import travel.entity.route_planning.RouteAttraction;
import travel.mapper.route_planning_mapper.RouteAttractionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 路线景点关联Repository接口
 */
@Repository
public class RouteAttractionRepository {

    @Autowired
    private RouteAttractionMapper routeAttractionMapper;

    /**
     * 保存路线景点关联
     */
    public RouteAttraction save(RouteAttraction routeAttraction) {
        routeAttractionMapper.insert(routeAttraction);
        return routeAttraction;
    }

    /**
     * 批量保存路线景点关联
     */
    public boolean batchSave(List<RouteAttraction> routeAttractions) {
        if (routeAttractions == null || routeAttractions.isEmpty()) {
            return false;
        }
        for (RouteAttraction routeAttraction : routeAttractions) {
            routeAttractionMapper.insert(routeAttraction);
        }
        return true;
    }

    /**
     * 更新路线景点关联
     */
    public boolean update(RouteAttraction routeAttraction) {
        return routeAttractionMapper.updateById(routeAttraction) > 0;
    }

    /**
     * 根据ID删除路线景点关联
     */
    public boolean deleteById(Long id) {
        return routeAttractionMapper.deleteById(id) > 0;
    }

    /**
     * 根据路线ID删除路线景点关联
     */
    public boolean deleteByRouteId(Long routeId) {
        QueryWrapper<RouteAttraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route_id", routeId);
        return routeAttractionMapper.delete(queryWrapper) > 0;
    }

    /**
     * 根据路线ID查询路线景点关联
     */
    public List<RouteAttraction> findByRouteId(Long routeId) {
        QueryWrapper<RouteAttraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route_id", routeId);
        queryWrapper.orderByAsc("sort_order");
        return routeAttractionMapper.selectList(queryWrapper);
    }

    /**
     * 根据景点ID查询路线景点关联
     */
    public List<RouteAttraction> findByAttractionId(Long attractionId) {
        QueryWrapper<RouteAttraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attraction_id", attractionId);
        return routeAttractionMapper.selectList(queryWrapper);
    }
}
