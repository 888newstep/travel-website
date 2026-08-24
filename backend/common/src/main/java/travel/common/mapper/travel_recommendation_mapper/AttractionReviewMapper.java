package travel.common.mapper.travel_recommendation_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import travel.common.entity.travel_recommendation.AttractionReview;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttractionReviewMapper extends BaseMapper<AttractionReview> {

    @Insert("""
            INSERT INTO attraction_review
                (attraction_id, user_id, rating, content, created_at, updated_at)
            VALUES
                (#{attractionId}, #{userId}, #{rating}, #{content}, #{createdAt}, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                rating = VALUES(rating),
                content = VALUES(content),
                updated_at = VALUES(updated_at),
                id = LAST_INSERT_ID(id)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsertReview(AttractionReview review);

    @Select("""
            SELECT rating, COUNT(*) AS rating_count
            FROM attraction_review
            WHERE attraction_id = #{attractionId}
            GROUP BY rating
            """)
    List<Map<String, Object>> selectRatingCounts(Integer attractionId);
}
