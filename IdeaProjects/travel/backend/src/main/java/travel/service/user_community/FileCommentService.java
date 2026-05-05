package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.FileComment;
import java.util.List;

public interface FileCommentService extends IService<FileComment> {

    List<FileComment> getByFileId(Integer fileId);

    List<FileComment> getByUserId(Integer userId);

    List<FileComment> getByParentId(Integer parentId);

    List<FileComment> getRecentComments(Integer limit);

    boolean addComment(Integer fileId, Integer userId, String userName, String content, Integer rating);

    boolean replyComment(Integer fileId, Integer userId, String userName, String content, Integer parentId);

    boolean likeComment(Integer commentId);

    boolean deleteComment(Integer commentId);

    double getAverageRatingByFileId(Integer fileId);

    int countCommentsByFileId(Integer fileId);
}