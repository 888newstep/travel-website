package travel.common.mapper.travel_recommendation_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.common.entity.travel_recommendation.ResourceFile;
import java.util.List;
import java.util.Map;

public interface ResourceFileMapper extends BaseMapper<ResourceFile> {

    List<ResourceFile> selectByUserId(Integer userId);

    List<ResourceFile> selectByFileType(String fileType);

    List<ResourceFile> selectByStatus(Integer status);

    List<ResourceFile> selectByFileName(String fileName);

    List<ResourceFile> selectByRouteId(Integer routeId);

    List<ResourceFile> selectByTags(String tags);

    List<ResourceFile> selectByCategory(String category);

    List<ResourceFile> selectByParentFileId(Integer parentFileId);

    List<ResourceFile> selectByMultipleConditions(Map<String, Object> conditions);

    List<ResourceFile> selectHotFiles(Integer limit);

    List<ResourceFile> selectNewFiles(Integer limit);

    int batchInsert(List<ResourceFile> files);

    int batchUpdate(List<ResourceFile> files);

    int batchDelete(List<Integer> ids);

    int incrementDownloadCount(Integer id);

    int incrementViewCount(Integer id);

    int updateRating(Integer id, Double rating);

    List<ResourceFile> selectByUserIdAndRouteId(Integer userId, Integer routeId);

    List<ResourceFile> selectByFileTypeAndStatus(String fileType, Integer status);
}