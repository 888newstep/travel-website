package travel.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.travel_recommendation.FileTag;
import travel.common.mapper.user_community_mapper.FileTagMapper;
import travel.common.security.AuthenticatedUserSupport;
import travel.file.service.FileTagService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FileTagServiceImpl extends ServiceImpl<FileTagMapper, FileTag> implements FileTagService {

    @Override
    public FileTag createFileTag(FileTag fileTag) {
        AuthenticatedUserSupport.requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        fileTag.setCreateTime(now);
        fileTag.setUpdateTime(now);
        save(fileTag);
        log.info("创建文件标签成功: id={}", fileTag.getId());
        return fileTag;
    }

    @Override
    public FileTag updateFileTag(Long id, FileTag fileTag) {
        AuthenticatedUserSupport.requireAdmin();
        if (getFileTag(id) == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        fileTag.setId(id.intValue());
        fileTag.setUpdateTime(LocalDateTime.now());
        updateById(fileTag);
        log.info("更新文件标签成功: id={}", id);
        return fileTag;
    }

    @Override
    public boolean deleteFileTag(Long id) {
        AuthenticatedUserSupport.requireAdmin();
        boolean deleted = removeById(id.intValue());
        log.info("删除文件标签: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    public FileTag getFileTag(Long id) {
        return getById(id.intValue());
    }

    @Override
    public List<FileTag> getAllCategories() {
        List<FileTag> categories = list();
        log.info("获取所有分类成功: count={}", categories.size());
        return categories;
    }

}
