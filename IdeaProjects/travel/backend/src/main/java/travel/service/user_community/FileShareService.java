package travel.service.user_community;

import travel.entity.travel_recommendation.ResourceFile;

import java.util.List;
import java.util.Map;

public interface FileShareService {

    /**
     * 生成文件分享链接
     */
    Map<String, Object> generateShareLink(Integer fileId, Integer expireHours, String password);

    /**
     * 通过分享链接获取文件
     */
    ResourceFile getFileByShareLink(String shareToken, String password);

    /**
     * 取消文件分享
     */
    boolean cancelShare(Integer fileId);

    /**
     * 获取文件的所有分享记录
     */
    List<Map<String, Object>> getFileShareRecords(Integer fileId);

    /**
     * 获取用户分享的所有文件
     */
    List<ResourceFile> getUserSharedFiles(Integer userId);

    /**
     * 更新分享设置
     */
    boolean updateShareSettings(Integer fileId, Integer expireHours, String password, boolean enableDownload);

    /**
     * 获取分享统计信息
     */
    Map<String, Object> getShareStatistics(Integer fileId);

    /**
     * 批量分享文件
     */
    Map<String, Object> batchShareFiles(List<Integer> fileIds, Integer expireHours, String password);
}
