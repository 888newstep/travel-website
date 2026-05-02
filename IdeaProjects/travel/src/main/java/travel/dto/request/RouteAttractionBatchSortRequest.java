// 包路径必须严格匹配 import 路径：org.example.travel.dto.request
package travel.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 路线景点批量排序请求DTO
 * 解决 "java: 程序包org.example.travel.dto.request不存在" 报错
 */
@Data
public class RouteAttractionBatchSortRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 路线ID */
    private Long routeId;

    /** 排序项列表（每个景点的天数/顺序/备注） */
    private List<SortItem> sortItems;

    /**
     * 排序子项（单个景点的调整参数）
     */
    @Data
    public static class SortItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 路线-景点关联ID（主键） */
        private Long relationId;

        /** 所属天数（第N天） */
        private Integer dayNumber;

        /** 当天访问顺序 */
        private Integer visitOrder;

        /** 备注（可选） */
        private String notes;
    }
}