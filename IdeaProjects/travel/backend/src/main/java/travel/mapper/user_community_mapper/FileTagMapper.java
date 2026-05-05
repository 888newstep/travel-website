package travel.mapper.user_community_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.entity.travel_recommendation.FileTag;
import java.util.List;

public interface FileTagMapper extends BaseMapper<FileTag> {

    List<FileTag> selectByFileId(Integer fileId);

    List<FileTag> selectByUserId(Integer userId);

    List<FileTag> selectByTagName(String tagName);

    List<FileTag> selectByTagType(String tagType);

    List<FileTag> selectPopularTags(Integer limit);

    int batchInsert(List<FileTag> tags);

    int batchDeleteByFileId(Integer fileId);

    int incrementUsageCount(Integer id);
}