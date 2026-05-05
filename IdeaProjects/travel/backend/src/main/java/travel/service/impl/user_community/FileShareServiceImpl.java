package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.ResourceFile;
import travel.service.user_community.FileShareService;
import travel.service.travel_recommendation.ResourceFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileShareServiceImpl implements FileShareService {

    @Autowired
    private ResourceFileService resourceFileService;

    // 内存缓存分享记录，实际项目中应使用Redis
    private final Map<String, ShareRecord> shareCache = new ConcurrentHashMap<>();

    private static class ShareRecord {
        Integer fileId;
        String password;
        LocalDateTime expireTime;
        boolean enableDownload;
        int accessCount;

        public ShareRecord(Integer fileId, String password, LocalDateTime expireTime, boolean enableDownload) {
            this.fileId = fileId;
            this.password = password;
            this.expireTime = expireTime;
            this.enableDownload = enableDownload;
            this.accessCount = 0;
        }
    }

    @Override
    public Map<String, Object> generateShareLink(Integer fileId, Integer expireHours, String password) {
        try {
            ResourceFile file = resourceFileService.getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在");
            }

            // 生成唯一分享令牌
            String shareToken = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime expireTime = LocalDateTime.now().plusHours(expireHours != null ? expireHours : 24);

            // 保存分享记录
            shareCache.put(shareToken, new ShareRecord(fileId, password, expireTime, true));

            // 更新文件分享信息
            file.setShareUrl(shareToken);
            file.setShareExpireTime(expireTime);
            resourceFileService.updateById(file);

            Map<String, Object> result = new HashMap<>();
            result.put("shareToken", shareToken);
            result.put("shareUrl", "/api/file-share/" + shareToken);
            result.put("expireTime", expireTime);
            result.put("fileId", fileId);
            result.put("fileName", file.getFileName());

            log.info("生成文件分享链接成功: fileId={}, shareToken={}, expireTime={}", fileId, shareToken, expireTime);
            return result;
        } catch (Exception e) {
            log.error("生成文件分享链接失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("生成分享链接失败: " + e.getMessage());
        }
    }

    @Override
    public ResourceFile getFileByShareLink(String shareToken, String password) {
        try {
            ShareRecord record = shareCache.get(shareToken);
            if (record == null) {
                throw new RuntimeException("分享链接不存在或已过期");
            }

            if (record.expireTime.isBefore(LocalDateTime.now())) {
                shareCache.remove(shareToken);
                throw new RuntimeException("分享链接已过期");
            }

            if (record.password != null && !record.password.equals(password)) {
                throw new RuntimeException("密码错误");
            }

            // 增加访问次数
            record.accessCount++;

            ResourceFile file = resourceFileService.getById(record.fileId);
            if (file == null) {
                shareCache.remove(shareToken);
                throw new RuntimeException("文件不存在");
            }

            log.info("通过分享链接访问文件成功: shareToken={}, fileId={}", shareToken, record.fileId);
            return file;
        } catch (Exception e) {
            log.error("通过分享链接访问文件失败: shareToken={}, error={}", shareToken, e.getMessage());
            throw new RuntimeException("访问分享文件失败: " + e.getMessage());
        }
    }

    @Override
    public boolean cancelShare(Integer fileId) {
        try {
            ResourceFile file = resourceFileService.getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在");
            }

            // 从缓存中移除分享记录
            if (file.getShareUrl() != null) {
                shareCache.remove(file.getShareUrl());
            }

            // 更新文件分享信息
            file.setShareUrl(null);
            file.setShareExpireTime(null);
            resourceFileService.updateById(file);

            log.info("取消文件分享成功: fileId={}", fileId);
            return true;
        } catch (Exception e) {
            log.error("取消文件分享失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("取消分享失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getFileShareRecords(Integer fileId) {
        try {
            List<Map<String, Object>> records = new ArrayList<>();
            
            // 从缓存中查找该文件的所有分享记录
            for (Map.Entry<String, ShareRecord> entry : shareCache.entrySet()) {
                if (entry.getValue().fileId.equals(fileId)) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("shareToken", entry.getKey());
                    record.put("expireTime", entry.getValue().expireTime);
                    record.put("accessCount", entry.getValue().accessCount);
                    record.put("enableDownload", entry.getValue().enableDownload);
                    records.add(record);
                }
            }

            log.info("获取文件分享记录成功: fileId={}, count={}", fileId, records.size());
            return records;
        } catch (Exception e) {
            log.error("获取文件分享记录失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取分享记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<ResourceFile> getUserSharedFiles(Integer userId) {
        try {
            // 查找用户分享的所有文件
            LambdaQueryWrapper<ResourceFile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ResourceFile::getUploadUserId, userId)
                    .isNotNull(ResourceFile::getShareUrl);
            
            List<ResourceFile> files = resourceFileService.list(queryWrapper);
            log.info("获取用户分享文件成功: userId={}, count={}", userId, files.size());
            return files;
        } catch (Exception e) {
            log.error("获取用户分享文件失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("获取分享文件失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateShareSettings(Integer fileId, Integer expireHours, String password, boolean enableDownload) {
        try {
            ResourceFile file = resourceFileService.getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在");
            }

            if (file.getShareUrl() == null) {
                throw new RuntimeException("文件未分享");
            }

            ShareRecord record = shareCache.get(file.getShareUrl());
            if (record == null) {
                throw new RuntimeException("分享记录不存在");
            }

            // 更新分享设置
            if (expireHours != null) {
                record.expireTime = LocalDateTime.now().plusHours(expireHours);
                file.setShareExpireTime(record.expireTime);
            }
            if (password != null) {
                record.password = password;
            }
            record.enableDownload = enableDownload;

            resourceFileService.updateById(file);
            log.info("更新文件分享设置成功: fileId={}", fileId);
            return true;
        } catch (Exception e) {
            log.error("更新文件分享设置失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("更新分享设置失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getShareStatistics(Integer fileId) {
        try {
            ResourceFile file = resourceFileService.getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在");
            }

            Map<String, Object> statistics = new HashMap<>();
            int totalShares = 0;
            int activeShares = 0;
            int totalAccesses = 0;

            // 统计分享信息
            for (ShareRecord record : shareCache.values()) {
                if (record.fileId.equals(fileId)) {
                    totalShares++;
                    if (record.expireTime.isAfter(LocalDateTime.now())) {
                        activeShares++;
                        totalAccesses += record.accessCount;
                    }
                }
            }

            statistics.put("fileId", fileId);
            statistics.put("fileName", file.getFileName());
            statistics.put("totalShares", totalShares);
            statistics.put("activeShares", activeShares);
            statistics.put("totalAccesses", totalAccesses);
            statistics.put("isShared", file.getShareUrl() != null);
            statistics.put("shareExpireTime", file.getShareExpireTime());

            log.info("获取文件分享统计成功: fileId={}", fileId);
            return statistics;
        } catch (Exception e) {
            log.error("获取文件分享统计失败: fileId={}, error={}", fileId, e.getMessage());
            throw new RuntimeException("获取分享统计失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> batchShareFiles(List<Integer> fileIds, Integer expireHours, String password) {
        try {
            List<Map<String, Object>> shareResults = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (Integer fileId : fileIds) {
                try {
                    Map<String, Object> result = generateShareLink(fileId, expireHours, password);
                    shareResults.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("fileId", fileId);
                    errorResult.put("error", e.getMessage());
                    shareResults.add(errorResult);
                    failCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalFiles", fileIds.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("shareResults", shareResults);

            log.info("批量分享文件完成: total={}, success={}, fail={}", fileIds.size(), successCount, failCount);
            return result;
        } catch (Exception e) {
            log.error("批量分享文件失败: error={}", e.getMessage());
            throw new RuntimeException("批量分享失败: " + e.getMessage());
        }
    }
}
