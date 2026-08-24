package travel.common.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import travel.common.dto.route.RouteVisitDailyAggregate;
import travel.common.entity.route_planning.RouteVisit;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RouteVisitMapper extends BaseMapper<RouteVisit> {

    @Select("""
            SELECT COUNT(*)
            FROM route_visit
            WHERE route_id = #{routeId}
              AND visit_date BETWEEN #{startDate} AND #{endDate}
            """)
    long countVisits(
            @Param("routeId") Integer routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(DISTINCT visitor_hash)
            FROM route_visit
            WHERE route_id = #{routeId}
              AND visit_date BETWEEN #{startDate} AND #{endDate}
            """)
    long countUniqueVisitors(
            @Param("routeId") Integer routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM (
                SELECT visitor_hash
                FROM route_visit
                WHERE route_id = #{routeId}
                  AND visit_date BETWEEN #{startDate} AND #{endDate}
                GROUP BY visitor_hash
                HAVING COUNT(DISTINCT visit_date) >= 2
            ) returning_visitors
            """)
    long countReturningVisitors(
            @Param("routeId") Integer routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT visit_date AS visitDate,
                   COUNT(*) AS visits,
                   COUNT(DISTINCT visitor_hash) AS uniqueVisitors
            FROM route_visit
            WHERE route_id = #{routeId}
              AND visit_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY visit_date
            ORDER BY visit_date
            """)
    List<RouteVisitDailyAggregate> selectDailyTrend(
            @Param("routeId") Integer routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
