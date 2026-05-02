package travel.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.entity.route_planning.RouteTransport;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RouteTransportMapper extends BaseMapper<RouteTransport> {

    /**
     * 根据路线ID查询交通信息
     * @param routeId 路线ID
     * @return 交通信息列表
     */
    List<RouteTransport> selectByRouteId(Long routeId);

    /**
     * 根据景点ID查询交通信息
     * @param attractionId 景点ID
     * @return 交通信息列表
     */
    List<RouteTransport> selectByAttractionId(Long attractionId);

    /**
     * 根据交通工具ID查询交通信息
     * @param transportId 交通工具ID
     * @return 交通信息列表
     */
    List<RouteTransport> selectByTransportId(Long transportId);
}
