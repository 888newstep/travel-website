package travel.attraction.service;

import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.utils.ThirdApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SyncRealtimeDataTask {

    private static final Logger log = LoggerFactory.getLogger(SyncRealtimeDataTask.class);

    private final AttractionRealtimeStatusService attractionRealtimeStatusService;
    private final ThirdApiUtil thirdApiUtil;

    /**
     * 每15分钟同步一次实时数据
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void syncRealtimeData() {
        log.info("开始同步景点实时数据...");
        
        try {
            // 1. 查询需要同步的景点状态
            List<AttractionRealtimeStatus> needSyncStatusList = attractionRealtimeStatusService.selectNeedSyncStatus(15);
            
            if (needSyncStatusList.isEmpty()) {
                log.info("暂无需要同步的景点数据");
                return;
            }
            
            log.info("需要同步 {} 个景点的实时数据", needSyncStatusList.size());
            
            // 2. 调用第三方API获取实时数据
            List<AttractionRealtimeStatus> updatedStatusList = thirdApiUtil.getRealtimeData(needSyncStatusList);
            
            // 3. 批量更新到数据库
            if (!updatedStatusList.isEmpty()) {
                boolean success = attractionRealtimeStatusService.batchUpdateStatus(updatedStatusList);
                log.info("同步景点实时数据{}", success ? "成功" : "失败");
            }
            
        } catch (Exception e) {
            log.error("同步景点实时数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天凌晨3点执行全量同步
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void fullSyncRealtimeData() {
        log.info("开始执行全量同步景点实时数据...");
        
        try {
            // 1. 获取所有景点状态
            List<AttractionRealtimeStatus> allStatusList = attractionRealtimeStatusService.list();
            
            if (allStatusList.isEmpty()) {
                log.info("暂无景点数据需要同步");
                return;
            }
            
            log.info("全量同步 {} 个景点的实时数据", allStatusList.size());
            
            // 2. 调用第三方API获取实时数据
            List<AttractionRealtimeStatus> updatedStatusList = thirdApiUtil.getRealtimeData(allStatusList);
            
            // 3. 批量更新到数据库
            if (!updatedStatusList.isEmpty()) {
                boolean success = attractionRealtimeStatusService.batchUpdateStatus(updatedStatusList);
                log.info("全量同步景点实时数据{}", success ? "成功" : "失败");
            }
            
        } catch (Exception e) {
            log.error("全量同步景点实时数据失败: {}", e.getMessage(), e);
        }
    }
}
