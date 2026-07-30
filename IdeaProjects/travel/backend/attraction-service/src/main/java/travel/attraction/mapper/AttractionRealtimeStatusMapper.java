package travel.attraction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景点实时状态Mapper
 */
public interface AttractionRealtimeStatusMapper extends BaseMapper<AttractionRealtimeStatus> {

    /**
     * 根据景点ID查询实时状态
     */
    @Select("SELECT * FROM attraction_realtime_status WHERE attraction_id = #{attractionId} AND deleted = 0 LIMIT 1")
    AttractionRealtimeStatus selectByAttractionId(Long attractionId);

    /**
     * 根据拥挤等级查询景点列表
     */
    @Select("SELECT * FROM attraction_realtime_status WHERE crowd_level >= #{minCrowdLevel} AND deleted = 0 ORDER BY crowd_level DESC")
    List<AttractionRealtimeStatus> selectByCrowdLevel(int minCrowdLevel);

    /**
     * 查询需要同步的景点状态
     */
    @Select("SELECT * FROM attraction_realtime_status WHERE update_time < #{threshold} AND deleted = 0")
    List<AttractionRealtimeStatus> selectNeedSync(LocalDateTime threshold);

    /**
     * 查询景点历史平均人流
     */
    @Select("SELECT AVG(crowd_count) FROM attraction_realtime_status WHERE attraction_id = #{attractionId} AND deleted = 0")
    Integer selectAvgCrowdCount(Long attractionId);

    /**
     * 批量更新同步时间
     */
    @Update("<script>" +
            "UPDATE attraction_realtime_status SET update_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateSyncTime(Long[] ids);
}
