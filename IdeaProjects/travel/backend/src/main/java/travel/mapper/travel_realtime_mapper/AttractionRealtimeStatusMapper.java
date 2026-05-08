package travel.mapper.travel_realtime_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 景点实时状态Mapper（Repository层）
 */
@Repository
public interface AttractionRealtimeStatusMapper extends BaseMapper<AttractionRealtimeStatus> {

    /**
     * 根据景点ID查询实时状态
     */
    @Select("SELECT * FROM attraction_realtime_status WHERE attraction_id = #{attractionId} AND deleted = 0 LIMIT 1")
    AttractionRealtimeStatus selectByAttractionId(@Param("attractionId") Long attractionId);

    /**
     * 查询拥挤景点列表
     */
    @Select("SELECT * FROM attraction_realtime_status WHERE crowd_level >= #{minCrowdLevel} AND deleted = 0 ORDER BY crowd_level DESC")
    List<AttractionRealtimeStatus> selectByCrowdLevel(@Param("minCrowdLevel") int minCrowdLevel);

    /**
     * 自定义SQL：查询景点近7天人流均值（降级备用）
     */
    Integer selectAvgCrowdCount(@Param("attractionId") Long attractionId);

    /**
     * 批量更新同步时间
     */
    int batchUpdateSyncTime(@Param("attractionIds") Long[] attractionIds);
}
