package travel.common.mapper.user_community_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.user_community.TravelNote;

public interface TravelNoteMapper extends BaseMapper<TravelNote> {

    @Select("SELECT * FROM travel_note WHERE id = #{noteId} FOR UPDATE")
    TravelNote selectByIdForUpdate(@Param("noteId") Integer noteId);

    @Update("UPDATE travel_note SET likes_count = COALESCE(likes_count, 0) + 1 WHERE id = #{noteId}")
    int incrementLikeCount(@Param("noteId") Integer noteId);

    @Update("UPDATE travel_note SET likes_count = GREATEST(COALESCE(likes_count, 0) - 1, 0) WHERE id = #{noteId}")
    int decrementLikeCount(@Param("noteId") Integer noteId);

    @Update("UPDATE travel_note SET views_count = COALESCE(views_count, 0) + 1 WHERE id = #{noteId}")
    int incrementViewCount(@Param("noteId") Integer noteId);
}
