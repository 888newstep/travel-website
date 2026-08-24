package travel.common.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.user_community.RouteShare;

/**
 * 路线分享Mapper
 */
public interface RouteShareMapper extends BaseMapper<RouteShare> {

    @Update("""
            UPDATE route_share
            SET visit_count = COALESCE(visit_count, 0) + 1
            WHERE id = #{shareId}
              AND is_active = TRUE
            """)
    int incrementVisitCount(@Param("shareId") Integer shareId);
}
