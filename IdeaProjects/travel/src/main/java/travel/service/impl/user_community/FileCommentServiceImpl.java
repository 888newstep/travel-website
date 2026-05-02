package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.FileComment;
import travel.mapper.user_community_mapper.FileCommentMapper;
import travel.service.user_community.FileCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FileCommentServiceImpl extends ServiceImpl<FileCommentMapper, FileComment> implements FileCommentService {

    @Autowired
    private FileCommentMapper fileCommentMapper;

    @Override
    public List<FileComment> getByFileId(Integer fileId) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getFileId, fileId)
                    .eq(FileComment::getStatus, 1)
                    .orderByDesc(FileComment::getCreateTime);
            List<FileComment> comments = fileCommentMapper.selectList(queryWrapper);
            log.info("获取文件评论成功: fileId={}, count={}", fileId, comments.size());
            return comments;
        } catch (Exception e) {
            log.error("获取文件评论失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取评论失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileComment> getByUserId(Integer userId) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getUserId, userId)
                    .eq(FileComment::getStatus, 1)
                    .orderByDesc(FileComment::getCreateTime);
            List<FileComment> comments = fileCommentMapper.selectList(queryWrapper);
            log.info("获取用户评论成功: userId={}, count={}", userId, comments.size());
            return comments;
        } catch (Exception e) {
            log.error("获取用户评论失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("获取评论失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileComment> getByParentId(Integer parentId) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getParentId, parentId)
                    .eq(FileComment::getStatus, 1)
                    .orderByAsc(FileComment::getCreateTime);
            List<FileComment> comments = fileCommentMapper.selectList(queryWrapper);
            log.info("获取评论回复成功: parentId={}, count={}", parentId, comments.size());
            return comments;
        } catch (Exception e) {
            log.error("获取评论回复失败: parentId={}, error={}", parentId, e.getMessage());
            throw new RuntimeException("获取回复失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileComment> getRecentComments(Integer limit) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getStatus, 1)
                    .eq(FileComment::getParentId, 0)
                    .orderByDesc(FileComment::getCreateTime)
                    .last("LIMIT " + limit);
            List<FileComment> comments = fileCommentMapper.selectList(queryWrapper);
            log.info("获取最近评论成功: limit={}, count={}", limit, comments.size());
            return comments;
        } catch (Exception e) {
            log.error("获取最近评论失败: limit={}, error={}", limit, e.getMessage());
            throw new RuntimeException("获取评论失败: " + e.getMessage());
        }
    }

    @Override
    public boolean addComment(Integer fileId, Integer userId, String userName, String content, Integer rating) {
        try {
            FileComment comment = new FileComment();
            comment.setFileId(fileId);
            comment.setUserId(userId);
            comment.setUserName(userName);
            comment.setContent(content);
            comment.setRating(rating);
            comment.setParentId(0);
            comment.setLikes(0);
            comment.setStatus(1);
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());

            boolean success = save(comment);
            log.info("添加评论成功: fileId={}, userId={}", fileId, userId);
            return success;
        } catch (Exception e) {
            log.error("添加评论失败: fileId={}, userId={}, error={}", fileId, userId, e.getMessage());
            throw new RuntimeException("添加评论失败: " + e.getMessage());
        }
    }

    @Override
    public boolean replyComment(Integer fileId, Integer userId, String userName, String content, Integer parentId) {
        try {
            // 检查父评论是否存在
            FileComment parentComment = getById(parentId);
            if (parentComment == null) {
                throw new RuntimeException("父评论不存在");
            }

            FileComment comment = new FileComment();
            comment.setFileId(fileId);
            comment.setUserId(userId);
            comment.setUserName(userName);
            comment.setContent(content);
            comment.setRating(0);
            comment.setParentId(parentId);
            comment.setLikes(0);
            comment.setStatus(1);
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());

            boolean success = save(comment);
            log.info("回复评论成功: fileId={}, userId={}, parentId={}", fileId, userId, parentId);
            return success;
        } catch (Exception e) {
            log.error("回复评论失败: fileId={}, userId={}, parentId={}, error={}", fileId, userId, parentId, e.getMessage());
            throw new RuntimeException("回复评论失败: " + e.getMessage());
        }
    }

    @Override
    public boolean likeComment(Integer commentId) {
        try {
            FileComment comment = getById(commentId);
            if (comment == null) {
                throw new RuntimeException("评论不存在");
            }

            comment.setLikes(Optional.ofNullable(comment.getLikes()).orElse(0) + 1);
            comment.setUpdateTime(LocalDateTime.now());

            boolean success = updateById(comment);
            log.info("点赞评论成功: commentId={}", commentId);
            return success;
        } catch (Exception e) {
            log.error("点赞评论失败: commentId={}, error={}", commentId, e.getMessage());
            throw new RuntimeException("点赞失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteComment(Integer commentId) {
        try {
            FileComment comment = getById(commentId);
            if (comment == null) {
                throw new RuntimeException("评论不存在");
            }

            // 软删除
            comment.setStatus(0);
            comment.setUpdateTime(LocalDateTime.now());

            boolean success = updateById(comment);
            log.info("删除评论成功: commentId={}", commentId);
            return success;
        } catch (Exception e) {
            log.error("删除评论失败: commentId={}, error={}", commentId, e.getMessage());
            throw new RuntimeException("删除评论失败: " + e.getMessage());
        }
    }

    @Override
    public double getAverageRatingByFileId(Integer fileId) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getFileId, fileId)
                    .eq(FileComment::getStatus, 1)
                    .gt(FileComment::getRating, 0);

            List<FileComment> comments = fileCommentMapper.selectList(queryWrapper);
            if (comments.isEmpty()) {
                return 0.0;
            }

            double totalRating = comments.stream()
                    .mapToInt(FileComment::getRating)
                    .sum();
            double averageRating = totalRating / comments.size();

            log.info("获取文件平均评分成功: fileId={}, averageRating={}", fileId, averageRating);
            return averageRating;
        } catch (Exception e) {
            log.error("获取文件平均评分失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取评分失败: " + e.getMessage());
        }
    }

    @Override
    public int countCommentsByFileId(Integer fileId) {
        try {
            LambdaQueryWrapper<FileComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileComment::getFileId, fileId)
                    .eq(FileComment::getStatus, 1);

            Long count = fileCommentMapper.selectCount(queryWrapper);
            log.info("获取文件评论数成功: fileId={}, count={}", fileId, count);
            return count.intValue();
        } catch (Exception e) {
            log.error("获取文件评论数失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取评论数失败: " + e.getMessage());
        }
    }
}
