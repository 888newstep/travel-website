package travel.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 路线调整VO
 */
@Data
public class RouteAdjustmentVO {

    /**
     * 调整类型：1-避开拥堵 2-缩短距离 3-减少时间 4-避开景点 5-添加景点
     */
    private Integer adjustmentType;

    /**
     * 避开的景点ID列表
     */
    private List<Long> avoidAttractionIds;

    /**
     * 添加的景点ID列表
     */
    private List<Long> addAttractionIds;

    /**
     * 调整后的路线点列表
     */
    private List<RoutePointVO> routePoints;

    /**
     * 调整原因
     */
    private String adjustmentReason;

    /**
     * 预计节省时间（分钟）
     */
    private Double estimatedTimeSaving;

    /**
     * 预计节省距离（公里）
     */
    private Double estimatedDistanceSaving;

    /**
     * 调整后的总距离（公里）
     */
    private Double adjustedTotalDistance;

    /**
     * 调整后的预计耗时（小时）
     */
    private Double adjustedEstimatedTime;

    /**
     * 调整后的预计成本（元）
     */
    private Double adjustedEstimatedCost;
}
