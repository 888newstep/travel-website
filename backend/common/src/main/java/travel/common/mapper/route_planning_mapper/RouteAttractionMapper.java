package travel.common.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.route_planning.RouteAttraction;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteAttractionMapper extends BaseMapper<RouteAttraction> {

    @Update("""
            UPDATE route_attractions
            SET visit_order = -id
            WHERE route_id = #{routeId}
            """)
    int reserveVisitOrders(@Param("routeId") Integer routeId);
}
