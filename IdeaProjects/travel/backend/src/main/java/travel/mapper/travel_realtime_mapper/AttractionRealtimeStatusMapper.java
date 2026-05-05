package travel.mapper.travel_realtime_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import org.springframework.stereotype.Repository;

/**
 * 景点实时状态Mapper（Repository层）
 */
@Repository // 标识为数据访问组件，Spring扫描管理
public interface AttractionRealtimeStatusMapper extends BaseMapper<AttractionRealtimeStatus> {

    /**
     * 自定义SQL：查询景点近7天人流均值（降级备用）
     * @param attractionId 景点ID
     * @return 人流均值
     */
    Integer selectAvgCrowdCount(@Param("attractionId") Long attractionId);

    /**
     * 批量更新同步时间
     * @param attractionIds 景点ID数组
     * @return 受影响行数
     */
    int batchUpdateSyncTime(@Param("attractionIds") Long[] attractionIds);
}