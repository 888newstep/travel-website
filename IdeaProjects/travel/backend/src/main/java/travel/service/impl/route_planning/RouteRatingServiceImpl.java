package travel.service.impl.route_planning;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import travel.entity.route_planning.RouteRating;
import travel.service.route_planning.RouteRatingService;
import org.springframework.stereotype.Service;
import travel.mapper.route_planning_mapper.RouteRatingMapper;

import java.util.List;

@Service
public class RouteRatingServiceImpl extends ServiceImpl<RouteRatingMapper, RouteRating> implements RouteRatingService {

    @Override
    public List<RouteRating> getByRouteId(Integer routeId) {
        QueryWrapper<RouteRating> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId);
        return list(wrapper);
    }

    @Override
    public List<RouteRating> getByUserId(Integer userId) {
        QueryWrapper<RouteRating> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return list(wrapper);
    }

    @Override
    public Double getAverageRating(Integer routeId) {
        List<RouteRating> ratings = getByRouteId(routeId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream()
                .mapToInt(RouteRating::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public Integer getRatingCount(Integer routeId) {
        QueryWrapper<RouteRating> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId);
        return (int) count(wrapper);
    }

    @Override
    public boolean hasRated(Integer routeId, Integer userId) {
        QueryWrapper<RouteRating> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .eq("user_id", userId);
        return count(wrapper) > 0;
    }

    @Override
    public RouteRating getUserRating(Integer routeId, Integer userId) {
        QueryWrapper<RouteRating> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId)
               .eq("user_id", userId);
        return getOne(wrapper);
    }
}
