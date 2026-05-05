package travel.entity.vo;

import lombok.Data;

/**
 * 热门路线VO
 */
@Data
public class TopRouteVO {

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
     * 排名
     */
    private Integer rank;
}
