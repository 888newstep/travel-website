package travel.mapper.user_community_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.entity.user_community.FileComment;
import java.util.List;

public interface FileCommentMapper extends BaseMapper<FileComment> {

    List<FileComment> selectByFileId(Integer fileId);

    List<FileComment> selectByUserId(Integer userId);

    List<FileComment> selectByParentId(Integer parentId);

    List<FileComment> selectByRating(Integer rating);

    List<FileComment> selectRecentComments(Integer limit);

    int incrementLikes(Integer id);

    double selectAverageRatingByFileId(Integer fileId);

    int countCommentsByFileId(Integer fileId);
}