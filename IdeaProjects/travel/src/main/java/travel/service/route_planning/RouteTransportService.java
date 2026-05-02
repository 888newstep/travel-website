package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.route_planning.RouteTransport;

import java.util.List;

/**
 * 路线交通服务接口
 * 职责：管理路线与交通方式的关联，提供交通方案规划和计算
 */
public interface RouteTransportService extends IService<RouteTransport> {

    /**
     * 根据路线ID查询交通信息
     * @param routeId 路线ID
     * @return 交通信息列表
     */
    List<RouteTransport> getByRouteId(Long routeId);

    /**
     * 根据景点ID查询交通信息
     * @param attractionId 景点ID
     * @return 交通信息列表
     */
    List<RouteTransport> getByAttractionId(Long attractionId);

    /**
     * 根据交通工具ID查询交通信息
     * @param transportId 交通工具ID
     * @return 交通信息列表
     */
    List<RouteTransport> getByTransportId(Long transportId);

    /**
     * 批量添加路线交通信息
     * @param routeTransports 交通信息列表
     * @return 是否添加成功
     */
    boolean batchAdd(List<RouteTransport> routeTransports);

    /**
     * 根据路线ID删除交通信息
     * @param routeId 路线ID
     * @return 是否删除成功
     */
    boolean deleteByRouteId(Long routeId);

    /**
     * 计算路线总交通费用
     * @param routeId 路线ID
     * @return 总交通费用
     */
    Double calculateTotalCost(Long routeId);

    /**
     * 计算路线总交通时间
     * @param routeId 路线ID
     * @return 总交通时间（分钟）
     */
    Integer calculateTotalTime(Long routeId);

    /**
     * 计算路线总距离
     * @param routeId 路线ID
     * @return 总距离（公里）
     */
    Double calculateTotalDistance(Long routeId);

    /**
     * 根据路线ID和交通方式查询交通信息
     * @param routeId 路线ID
     * @param transportType 交通方式
     * @return 交通信息列表
     */
    List<RouteTransport> getByRouteIdAndType(Long routeId, String transportType);

    /**
     * 统计路线的交通方式分布
     * @param routeId 路线ID
     * @return 交通方式到数量的映射
     */
    java.util.Map<String, Integer> countTransportTypesByRouteId(Long routeId);

    /**
     * 根据起始和结束景点ID查询交通信息
     * @param fromAttractionId 起始景点ID
     * @param toAttractionId 结束景点ID
     * @return 交通信息列表
     */
    List<RouteTransport> getByFromAndToAttractionId(Long fromAttractionId, Long toAttractionId);
}
