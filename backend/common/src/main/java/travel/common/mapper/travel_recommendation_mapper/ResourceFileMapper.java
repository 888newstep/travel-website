package travel.common.mapper.travel_recommendation_mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import travel.common.entity.travel_recommendation.ResourceFile;

public interface ResourceFileMapper extends BaseMapper<ResourceFile> {

    @Update("UPDATE resource_file SET download_count = COALESCE(download_count, 0) + 1, last_access_time = CURRENT_TIMESTAMP WHERE id = #{fileId} AND status = 1")
    int incrementDownloadCount(@Param("fileId") Integer fileId);
}
