package travel.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.common.entity.route_planning.Route;
import org.springframework.stereotype.Repository;

/**
 * 路线Mapper接口
 */
@Repository
public interface RouteMapper extends BaseMapper<Route> {
}
