package travel.service.travel_recommendation;

import travel.entity.travel_recommendation.ResourceFile;

import java.util.List;
import java.util.Map;

public interface FileVersionService {

    /**
     * 获取文件的所有版本
     */
    List<ResourceFile> getFileVersions(Integer fileId);

    /**
     * 获取指定版本的文件
     */
    ResourceFile getFileVersion(Integer fileId, Integer version);

    /**
     * 创建文件新版本
     */
    Map<String, Object> createNewVersion(Integer fileId, String fileName, String description);

    /**
     * 回滚到指定版本
     */
    boolean revertToVersion(Integer fileId, Integer version);

    /**
     * 删除指定版本
     */
    boolean deleteVersion(Integer fileId, Integer version);

    /**
     * 获取版本差异
     */
    Map<String, Object> getVersionDiff(Integer fileId, Integer oldVersion, Integer newVersion);

    /**
     * 批量操作版本
     */
    Map<String, Object> batchOperateVersions(List<Integer> fileIds, String operation, Integer version);

    /**
     * 获取版本统计信息
     */
    Map<String, Object> getVersionStatistics(Integer fileId);
}
