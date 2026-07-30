package travel.common.mapper.user_community_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.common.entity.user_community.FileComment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FileCommentMapper extends BaseMapper<FileComment> {

    @Select("SELECT * FROM file_comment WHERE file_id = #{fileId} ORDER BY create_time DESC")
    List<FileComment> selectByFileId(@Param("fileId") Integer fileId);

    @Select("SELECT COALESCE(AVG(rating), 0.0) FROM file_comment WHERE file_id = #{fileId} AND status = 1")
    Double selectAverageRatingByFileId(@Param("fileId") Integer fileId);

    @Update("UPDATE file_comment SET likes = likes + 1 WHERE id = #{commentId}")
    int incrementLikes(@Param("commentId") Integer commentId);
}