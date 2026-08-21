package travel.common.mapper.travel_recommendation_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.travel_recommendation.Attraction;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AttractionMapper extends BaseMapper<Attraction> {

    List<Attraction> selectEnableAndSyncOpenAttractions();

    IPage<Attraction> selectAttractionPage(
            @Param("page") Page<Attraction> page,
            @Param("cityId") Long cityId,
            @Param("type") String type
    );

    Attraction selectLatLngById(@Param("attractionId") Long attractionId);

    List<Attraction> selectByCursor(
            @Param("cityId") Integer cityId,
            @Param("lastRating") BigDecimal lastRating,
            @Param("lastId") Integer lastId,
            @Param("size") int size
    );

    List<Attraction> selectAllByCursor(
            @Param("lastRating") BigDecimal lastRating,
            @Param("lastId") Integer lastId,
            @Param("size") int size
    );

    List<Attraction> selectByOffset(
            @Param("cityId") Integer cityId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    List<Attraction> selectAllByOffset(
            @Param("offset") int offset,
            @Param("size") int size
    );

    @Update("UPDATE attraction SET view_count = COALESCE(view_count, 0) + 1 WHERE id = #{attractionId}")
    int incrementViewCount(@Param("attractionId") Integer attractionId);
}
