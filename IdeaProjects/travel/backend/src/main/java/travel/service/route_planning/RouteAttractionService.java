package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.dto.request.RouteAttractionBatchSortRequest;
import travel.entity.route_planning.RouteAttraction;
import travel.exception.BusinessException;

import java.util.List;

/**
 * 路线-景点关联关系服务接口
 * 职责：管理路线与景点的绑定、排序、查询
 */
public interface RouteAttractionService extends IService<RouteAttraction> {

    /**
     * 根据景点ID查询关联的所有路线ID
     * @param attractionId 景点ID
     * @return 路线ID列表
     */
    List<Long> getRouteIdsByAttractionId(Long attractionId);

    /**
     * 批量拖拽排序路线景点（调整天数/顺序）
     * @param request 排序请求
     * @return 是否排序成功
     * @throws BusinessException 顺序重复/参数错误时抛出
     */
    boolean batchSortRouteAttractions(RouteAttractionBatchSortRequest request);

    /**
     * 根据路线ID查询关联的景点列表（按天数+访问顺序排序）
     * @param routeId 路线ID
     * @return 路线景点列表
     */
    List<RouteAttraction> getByRouteIdOrderByDayAndVisit(Long routeId);

    /**
     * 根据路线ID查询关联的景点列表（按天数+访问顺序排序）
     * @param routeId 路线ID
     * @return 路线景点列表
     */
    default List<RouteAttraction> getByRouteIdOrderByDayAndVisit(Integer routeId) {
        return getByRouteIdOrderByDayAndVisit(routeId != null ? routeId.longValue() : null);
    }

    /**
     * 根据景点ID查询所有关联的路线-景点关系
     * @param attractionId 景点ID
     * @return 路线-景点关系列表
     */
    List<RouteAttraction> getByAttractionId(Integer attractionId);

    /**
     * 统计每个景点出现在多少条路线中
     * @param attractionIds 景点ID列表
     * @return 景点ID到出现次数的映射
     */
    java.util.Map<Integer, Integer> countRouteOccurrences(java.util.List<Integer> attractionIds);
}
