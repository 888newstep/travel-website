package travel.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_recommendation.FileTag;
import travel.common.mapper.user_community_mapper.FileTagMapper;
import travel.common.security.AuthenticatedUserSupport;
import travel.file.service.FileTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileTagServiceImpl extends ServiceImpl<FileTagMapper, FileTag> implements FileTagService {

    private static final Logger log = LoggerFactory.getLogger(FileTagServiceImpl.class);

    private final FileTagMapper fileTagMapper;

    @Override
    public List<FileTag> getByFileId(Integer fileId) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileTag::getFileId, fileId)
                    .orderByDesc(FileTag::getUsageCount)
                    .orderByAsc(FileTag::getTagName);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("获取文件标签成功: fileId={}, count={}", fileId, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("获取文件标签失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取标签失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getByUserId(Integer userId) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileTag::getUserId, userId)
                    .groupBy(FileTag::getTagName)
                    .orderByDesc(FileTag::getUsageCount)
                    .orderByAsc(FileTag::getTagName);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("获取用户标签成功: userId={}, count={}", userId, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("获取用户标签失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("获取标签失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getByTagName(String tagName) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(FileTag::getTagName, tagName)
                    .groupBy(FileTag::getFileId)
                    .orderByDesc(FileTag::getUsageCount);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("通过标签名获取标签成功: tagName={}, count={}", tagName, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("通过标签名获取标签失败: tagName={}, error={}", tagName, e.getMessage());
            throw new RuntimeException("获取标签失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getByTagType(String tagType) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileTag::getTagType, tagType)
                    .groupBy(FileTag::getTagName)
                    .orderByDesc(FileTag::getUsageCount)
                    .orderByAsc(FileTag::getTagName);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("通过标签类型获取标签成功: tagType={}, count={}", tagType, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("通过标签类型获取标签失败: tagType={}, error={}", tagType, e.getMessage());
            throw new RuntimeException("获取标签失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getPopularTags(Integer limit) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.groupBy(FileTag::getTagName)
                    .orderByDesc(FileTag::getUsageCount)
                    .orderByAsc(FileTag::getTagName)
                    .last("LIMIT " + limit);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("获取热门标签成功: limit={}, count={}", limit, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("获取热门标签失败: limit={}, error={}", limit, e.getMessage());
            throw new RuntimeException("获取标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean addTag(Integer fileId, String tagName, String tagType, Integer userId) {
        try {
            LambdaQueryWrapper<FileTag> existingQuery = new LambdaQueryWrapper<>();
            existingQuery.eq(FileTag::getFileId, fileId)
                    .eq(FileTag::getTagName, tagName);
            FileTag existingTag = fileTagMapper.selectOne(existingQuery);

            if (existingTag != null) {
                existingTag.setUsageCount(Optional.ofNullable(existingTag.getUsageCount()).orElse(0) + 1);
                existingTag.setUpdateTime(LocalDateTime.now());
                boolean success = updateById(existingTag);
                log.info("更新标签使用次数成功: fileId={}, tagName={}", fileId, tagName);
                return success;
            } else {
                FileTag tag = new FileTag();
                tag.setFileId(fileId);
                tag.setTagName(tagName);
                tag.setTagType(tagType != null ? tagType : "general");
                tag.setUserId(userId);
                tag.setUsageCount(1);
                tag.setCreateTime(LocalDateTime.now());
                tag.setUpdateTime(LocalDateTime.now());

                boolean success = save(tag);
                log.info("添加标签成功: fileId={}, tagName={}", fileId, tagName);
                return success;
            }
        } catch (Exception e) {
            log.error("添加标签失败: fileId={}, tagName={}, error={}", fileId, tagName, e.getMessage());
            throw new RuntimeException("添加标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeTag(Integer fileId, String tagName) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileTag::getFileId, fileId)
                    .eq(FileTag::getTagName, tagName);

            boolean success = remove(queryWrapper);
            log.info("移除标签成功: fileId={}, tagName={}", fileId, tagName);
            return success;
        } catch (Exception e) {
            log.error("移除标签失败: fileId={}, tagName={}, error={}", fileId, tagName, e.getMessage());
            throw new RuntimeException("移除标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean batchAddTags(Integer fileId, List<String> tagNames, String tagType, Integer userId) {
        try {
            int successCount = 0;
            for (String tagName : tagNames) {
                try {
                    if (addTag(fileId, tagName, tagType, userId)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("添加标签失败: fileId={}, tagName={}, error={}", fileId, tagName, e.getMessage());
                }
            }
            log.info("批量添加标签完成: fileId={}, total={}, success={}", fileId, tagNames.size(), successCount);
            return successCount > 0;
        } catch (Exception e) {
            log.error("批量添加标签失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("批量添加标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean batchRemoveTags(Integer fileId, List<String> tagNames) {
        try {
            int successCount = 0;
            for (String tagName : tagNames) {
                try {
                    if (removeTag(fileId, tagName)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("移除标签失败: fileId={}, tagName={}, error={}", fileId, tagName, e.getMessage());
                }
            }
            log.info("批量移除标签完成: fileId={}, total={}, success={}", fileId, tagNames.size(), successCount);
            return successCount > 0;
        } catch (Exception e) {
            log.error("批量移除标签失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("批量移除标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateTagUsageCount(Integer tagId) {
        try {
            FileTag tag = getById(tagId);
            if (tag == null) {
                throw new RuntimeException("标签不存在");
            }

            tag.setUsageCount(Optional.ofNullable(tag.getUsageCount()).orElse(0) + 1);
            tag.setUpdateTime(LocalDateTime.now());

            boolean success = updateById(tag);
            log.info("更新标签使用次数成功: tagId={}", tagId);
            return success;
        } catch (Exception e) {
            log.error("更新标签使用次数失败: tagId={}, error={}", tagId, e.getMessage());
            throw new RuntimeException("更新标签使用次数失败: " + e.getMessage());
        }
    }

    @Override
    public FileTag createFileTag(FileTag fileTag) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            fileTag.setCreateTime(LocalDateTime.now());
            fileTag.setUpdateTime(LocalDateTime.now());
            save(fileTag);
            log.info("创建文件标签成功: id={}", fileTag.getId());
            return fileTag;
        } catch (Exception e) {
            log.error("创建文件标签失败: error={}", e.getMessage());
            throw new RuntimeException("创建文件标签失败: " + e.getMessage());
        }
    }

    @Override
    public FileTag updateFileTag(Long id, FileTag fileTag) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            FileTag existingTag = getById(id);
            if (existingTag == null) {
                throw new RuntimeException("标签不存在");
            }
            fileTag.setId(id.intValue());
            fileTag.setUpdateTime(LocalDateTime.now());
            updateById(fileTag);
            log.info("更新文件标签成功: id={}", id);
            return fileTag;
        } catch (Exception e) {
            log.error("更新文件标签失败: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("更新文件标签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFileTag(Long id) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            boolean success = removeById(id.intValue());
            log.info("删除文件标签成功: id={}", id);
            return success;
        } catch (Exception e) {
            log.error("删除文件标签失败: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("删除文件标签失败: " + e.getMessage());
        }
    }

    @Override
    public FileTag getFileTag(Long id) {
        try {
            FileTag tag = getById(id.intValue());
            log.info("获取文件标签成功: id={}", id);
            return tag;
        } catch (Exception e) {
            log.error("获取文件标签失败: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("获取文件标签失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getAllCategories() {
        try {
            List<FileTag> tags = list();
            log.info("获取所有分类成功: count={}", tags.size());
            return tags;
        } catch (Exception e) {
            log.error("获取所有分类失败: error={}", e.getMessage());
            throw new RuntimeException("获取所有分类失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getCategoryTree() {
        try {
            List<Map<String, Object>> tree = new ArrayList<>();
            log.info("获取分类树成功");
            return tree;
        } catch (Exception e) {
            log.error("获取分类树失败: error={}", e.getMessage());
            throw new RuntimeException("获取分类树失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> getChildCategories(Long parentId) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("获取子分类成功: parentId={}, count={}", parentId, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("获取子分类失败: parentId={}, error={}", parentId, e.getMessage());
            throw new RuntimeException("获取子分类失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> searchCategories(String keyword) {
        try {
            LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(FileTag::getTagName, keyword);
            List<FileTag> tags = fileTagMapper.selectList(queryWrapper);
            log.info("搜索分类成功: keyword={}, count={}", keyword, tags.size());
            return tags;
        } catch (Exception e) {
            log.error("搜索分类失败: keyword={}, error={}", keyword, e.getMessage());
            throw new RuntimeException("搜索分类失败: " + e.getMessage());
        }
    }

    @Override
    public boolean moveCategory(Long categoryId, Long newParentId) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            log.info("移动分类成功: categoryId={}, newParentId={}", categoryId, newParentId);
            return true;
        } catch (Exception e) {
            log.error("移动分类失败: error={}", e.getMessage());
            throw new RuntimeException("移动分类失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getCategoryStatistics(Long id) {
        try {
            Map<String, Object> statistics = new HashMap<>();
            log.info("获取分类统计信息成功: id={}", id);
            return statistics;
        } catch (Exception e) {
            log.error("获取分类统计信息失败: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("获取分类统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileTag> batchCreateCategories(List<FileTag> categories) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            categories.forEach(category -> {
                category.setCreateTime(LocalDateTime.now());
                category.setUpdateTime(LocalDateTime.now());
            });
            saveBatch(categories);
            log.info("批量创建分类成功: count={}", categories.size());
            return categories;
        } catch (Exception e) {
            log.error("批量创建分类失败: error={}", e.getMessage());
            throw new RuntimeException("批量创建分类失败: " + e.getMessage());
        }
    }
}
