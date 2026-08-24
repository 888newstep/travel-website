package travel.common.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.route_planning.Route;
import org.springframework.stereotype.Repository;

/**
 * 路线Mapper接口
 */
@Repository
public interface RouteMapper extends BaseMapper<Route> {

    @Update("""
            UPDATE route
            SET view_count = COALESCE(view_count, 0) + 1
            WHERE id = #{routeId}
            """)
    int incrementViewCount(@Param("routeId") Integer routeId);
}
