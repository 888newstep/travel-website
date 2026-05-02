package travel.service.travel_realtime;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.exception.BusinessException;

import java.util.List;

/**
 * 景点实时状态服务接口
 * 职责：管理景点实时天气、人流、温度等数据的CRUD+批量同步+统计
 */
public interface AttractionRealtimeStatusService extends IService<AttractionRealtimeStatus> {

    /**
     * 根据景点ID查询实时状态
     * @param attractionId 景点ID
     * @return 实时状态对象
     * @throws BusinessException 景点无实时数据时抛出异常
     */
    AttractionRealtimeStatus getByAttractionId(Long attractionId);

    /**
     * 批量更新景点实时状态
     * @param statusList 实时状态列表
     * @return 是否更新成功
     */
    boolean batchUpdateStatus(List<AttractionRealtimeStatus> statusList);

    /**
     * 查询景点历史人流均值（降级备用）
     * @param attractionId 景点ID
     * @return 历史人流均值
     */
    Integer selectAvgCrowdCount(Long attractionId);

    /**
     * 查询需要同步的景点状态（1小时内未更新/数据异常）
     * @param minutes 未更新分钟数
     * @return 待同步状态列表
     */
    List<AttractionRealtimeStatus> selectNeedSyncStatus(Integer minutes);

    /**
     * 批量获取景点实时状态
     * @param attractionIds 景点ID列表
     * @return 实时状态列表
     */
    List<AttractionRealtimeStatus> getByAttractionIds(List<Long> attractionIds);

    /**
     * 更新或保存景点实时状态
     * @param status 实时状态对象
     * @return 是否操作成功
     */
    boolean updateOrSave(AttractionRealtimeStatus status);

    /**
     * 获取活跃的预警信息
     * @return 预警信息列表
     */
    List<Object> getActiveWarns();

    /**
     * 批量更新同步时间
     * @param attractionIds 景点ID数组
     * @return 受影响行数
     */
    int batchUpdateSyncTime(Long[] attractionIds);

    /**
     * 查询景点近7天人流均值（使用attractionRealtimeStatusMapper）
     * @param attractionId 景点ID
     * @return 人流均值
     */
    Integer selectAvgCrowdCountUsingMapper(Long attractionId);
}
