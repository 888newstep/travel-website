package travel.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import travel.common.entity.user_community.RouteComment;
import travel.route.service.RouteRatingService;
import org.springframework.stereotype.Service;
import travel.common.mapper.route_planning_mapper.RouteCommentMapper;

import java.util.List;

@Service
public class RouteRatingServiceImpl extends ServiceImpl<RouteCommentMapper, RouteComment> implements RouteRatingService {

    @Override
    public List<RouteComment> getByRouteId(Integer routeId) {
        QueryWrapper<RouteComment> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .isNotNull("rating");
        return list(wrapper);
    }

    @Override
    public List<RouteComment> getByUserId(Integer userId) {
        QueryWrapper<RouteComment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .isNotNull("rating");
        return list(wrapper);
    }

    @Override
    public Double getAverageRating(Integer routeId) {
        List<RouteComment> ratings = getByRouteId(routeId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream()
                .filter(r -> r.getRating() != null)
                .mapToDouble(RouteComment::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public Integer getRatingCount(Integer routeId) {
        QueryWrapper<RouteComment> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .isNotNull("rating");
        return (int) count(wrapper);
    }

    @Override
    public boolean hasRated(Integer routeId, Integer userId) {
        QueryWrapper<RouteComment> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .eq("user_id", userId)
               .isNotNull("rating");
        return count(wrapper) > 0;
    }

    @Override
    public RouteComment getUserRating(Integer routeId, Integer userId) {
        QueryWrapper<RouteComment> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .eq("user_id", userId)
               .isNotNull("rating");
        return getOne(wrapper);
    }
}