package travel.common.mapper.route_planning_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.user_community.RouteComment;

/**
 * 路线评价Mapper
 */
public interface RouteCommentMapper extends BaseMapper<RouteComment> {

    @Select("""
            SELECT COUNT(*)
            FROM user_collection
            WHERE user_id = #{userId}
              AND item_id = #{commentId}
              AND item_type = 'route_comment'
              AND collection_type = 'like'
            """)
    int countCommentLike(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Insert("""
            INSERT INTO user_collection
                (user_id, item_id, item_type, collection_type, is_public, created_at)
            VALUES
                (#{userId}, #{commentId}, 'route_comment', 'like', FALSE, CURRENT_TIMESTAMP)
            """)
    int insertCommentLike(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Delete("""
            DELETE FROM user_collection
            WHERE user_id = #{userId}
              AND item_id = #{commentId}
              AND item_type = 'route_comment'
              AND collection_type = 'like'
            """)
    int deleteCommentLike(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Delete("""
            DELETE FROM user_collection
            WHERE item_id = #{commentId}
              AND item_type = 'route_comment'
              AND collection_type = 'like'
            """)
    int deleteAllCommentLikes(@Param("commentId") Integer commentId);

    @Update("""
            UPDATE route_comment
            SET likes_count = COALESCE(likes_count, 0) + 1
            WHERE id = #{commentId}
            """)
    int incrementCommentLikeCount(@Param("commentId") Integer commentId);

    @Update("""
            UPDATE route_comment
            SET likes_count = GREATEST(COALESCE(likes_count, 0) - 1, 0)
            WHERE id = #{commentId}
            """)
    int decrementCommentLikeCount(@Param("commentId") Integer commentId);

    @Select("SELECT COALESCE(likes_count, 0) FROM route_comment WHERE id = #{commentId}")
    Integer selectCommentLikeCount(@Param("commentId") Integer commentId);
}
