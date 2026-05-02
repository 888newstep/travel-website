package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import travel.entity.route_planning.Route;
import travel.mapper.route_planning_mapper.RouteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 路线Repository接口
 */
@Repository
public class RouteRepository {

    @Autowired
    private RouteMapper routeMapper;

    /**
     * 保存路线
     */
    public Route save(Route route) {
        routeMapper.insert(route);
        return route;
    }

    /**
     * 更新路线
     */
    public boolean update(Route route) {
        return routeMapper.updateById(route) > 0;
    }

    /**
     * 根据ID删除路线
     */
    public boolean deleteById(Long id) {
        return routeMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询路线
     */
    public Optional<Route> findById(Long id) {
        Route route = routeMapper.selectById(id);
        return Optional.ofNullable(route);
    }

    /**
     * 查询所有路线
     */
    public List<Route> findAll() {
        return routeMapper.selectList(null);
    }

    /**
     * 根据条件查询路线
     */
    public List<Route> findByCondition(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Route> queryWrapper) {
        return routeMapper.selectList(queryWrapper);
    }

    /**
     * 分页查询路线
     */
    public Page<Route> findByPage(Page<Route> page, QueryWrapper<Route> queryWrapper) {
        return routeMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据用户ID查询路线
     */
    public List<Route> findByUserId(Long userId) {
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return routeMapper.selectList(queryWrapper);
    }

    /**
     * 根据城市ID查询路线
     */
    public List<Route> findByCityId(Long cityId) {
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        return routeMapper.selectList(queryWrapper);
    }
}
