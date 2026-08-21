package travel.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.travel_recommendation.FileTag;

import java.util.List;

public interface FileTagService extends IService<FileTag> {

    FileTag createFileTag(FileTag fileTag);

    FileTag updateFileTag(Long id, FileTag fileTag);

    boolean deleteFileTag(Long id);

    FileTag getFileTag(Long id);

    List<FileTag> getAllCategories();

}
