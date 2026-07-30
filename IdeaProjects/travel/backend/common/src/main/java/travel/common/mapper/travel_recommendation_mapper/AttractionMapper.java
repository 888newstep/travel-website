package travel.common.mapper.travel_recommendation_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import travel.common.entity.travel_recommendation.Attraction;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 景点基础信息Mapper（Repository层）
 * 解决 SyncRealtimeDataTask 中 "找不到符号 AttractionMapper" 报错
 */
@Repository // 标识为数据访问组件，Spring扫描管理
public interface AttractionMapper extends BaseMapper<Attraction> {

    /**
     * 自定义SQL：查询所有启用且开启同步的景点（同步任务核心方法）
     * @return 待同步的景点列表
     */
    /*
    @Select("""
        SELECT id, name, city_id, latitude, longitude, type 
        FROM attractions
        WHERE status = 1 
        AND sync_switch = 1 
        AND deleted = 0
    """)

     */
    List<Attraction> selectEnableAndSyncOpenAttractions();

    /**
     * 分页查询景点（扩展方法，适配后台管理）
     * @param page 分页参数
     * @param cityId 城市ID（可选）
     * @param type 景点类型（可选）
     * @return 分页结果
     */
    IPage<Attraction> selectAttractionPage(
            @Param("page") Page<Attraction> page,
            @Param("cityId") Long cityId,
            @Param("type") String type
    );

    /**
     * 查询景点经纬度（仅同步任务需要的字段，优化性能）
     * @param attractionId 景点ID
     * @return 景点对象（仅含id、latitude、longitude）
     */
    /*
    @Select("""
        SELECT id, latitude, longitude 
        FROM attractions
        WHERE id = #{attractionId} 
        AND deleted = 0
    """)

     */
    Attraction selectLatLngById(@Param("attractionId") Long attractionId);
}
