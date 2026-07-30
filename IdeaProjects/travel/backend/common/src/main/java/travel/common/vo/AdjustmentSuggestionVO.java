package travel.common.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 路线调整建议VO
 */
@Data
public class AdjustmentSuggestionVO {

    /**
     * 是否需要调整
     */
    private Boolean needAdjustment;

    /**
     * 调整建议类型：1-避开拥堵 2-缩短距离 3-减少时间 4-避开景点 5-添加景点
     */
    private List<Integer> suggestionTypes;

    /**
     * 详细建议
     */
    private String detailedSuggestion;

    /**
     * 拥堵路段信息
     */
    private List<Map<String, Object>> congestionSegments;

    /**
     * 景点排队情况
     */
    private Map<Long, Map<String, Object>> attractionQueueInfo;

    /**
     * 备选路线数量
     */
    private Integer alternativeRouteCount;

    /**
     * 预计调整效果
     */
    private Map<String, Object> estimatedEffect;
}
