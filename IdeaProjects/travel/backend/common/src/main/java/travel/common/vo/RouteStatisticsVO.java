package travel.common.vo;

import lombok.Data;

/**
 * 路线统计信息VO
 */
@Data
public class RouteStatisticsVO {

    /**
     * 路线ID
     */
    private Long routeId;

    /**
     * 路线名称
     */
    private String routeName;

    /**
     * 访问次数
     */
    private Integer visitCount;

    /**
     * 收藏次数
     */
    private Integer collectionCount;

    /**
     * 分享次数
     */
    private Integer shareCount;

    /**
     * 评价次数
     */
    private Integer commentCount;

    /**
     * 平均评分
     */
    private Double averageScore;

    /**
     * 路线类型
     */
    private String routeType;

    /**
     * 路线长度（公里）
     */
    private Double routeLength;

    /**
     * 预计耗时（小时）
     */
    private Double estimatedTime;

    /**
     * 预计成本（元）
     */
    private Double estimatedCost;

    /**
     * 创建时间
     */
    private String createTime;
}
