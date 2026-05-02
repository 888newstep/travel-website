package travel.controller.travel_realtime_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.service.travel_realtime.AttractionRealtimeStatusService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/realtime-status")
@RequiredArgsConstructor
public class RealtimeStatusController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeStatusController.class);

    private final AttractionRealtimeStatusService attractionRealtimeStatusService;

    /**
     * 获取景点实时状态
     * GET /api/realtime-status/attraction/{attractionId}
     */
    @GetMapping("/attraction/{attractionId}")
    public Result<AttractionRealtimeStatus> getAttractionRealtimeStatus(@PathVariable Long attractionId) {
        try {
            log.info("获取景点实时状态请求: attractionId={}", attractionId);
            AttractionRealtimeStatus status = attractionRealtimeStatusService.getByAttractionId(attractionId);
            return Result.success("获取实时状态成功", status);
        } catch (Exception e) {
            log.error("获取景点实时状态失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量获取景点实时状态
     * POST /api/realtime-status/batch
     */
    @PostMapping("/batch")
    public Result<List<AttractionRealtimeStatus>> getBatchRealtimeStatus(@RequestBody List<Long> attractionIds) {
        try {
            log.info("批量获取景点实时状态请求: attractionIds={}", attractionIds);
            List<AttractionRealtimeStatus> statusList = attractionRealtimeStatusService.getByAttractionIds(attractionIds);
            return Result.success("批量获取实时状态成功", statusList);
        } catch (Exception e) {
            log.error("批量获取景点实时状态失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新或保存景点实时状态
     * POST /api/realtime-status/update
     */
    @PostMapping("/update")
    public Result<Boolean> updateRealtimeStatus(@RequestBody AttractionRealtimeStatus status) {
        try {
            log.info("更新景点实时状态请求: attractionId={}", status.getAttractionId());
            boolean result = attractionRealtimeStatusService.updateOrSave(status);
            return Result.success("更新实时状态成功", result);
        } catch (Exception e) {
            log.error("更新景点实时状态失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量更新景点实时状态
     * POST /api/realtime-status/batch-update
     */
    @PostMapping("/batch-update")
    public Result<Boolean> batchUpdateRealtimeStatus(@RequestBody List<AttractionRealtimeStatus> statusList) {
        try {
            log.info("批量更新景点实时状态请求: count={}", statusList.size());
            boolean result = attractionRealtimeStatusService.batchUpdateStatus(statusList);
            return Result.success("批量更新实时状态成功", result);
        } catch (Exception e) {
            log.error("批量更新景点实时状态失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取景点历史人流均值
     * GET /api/realtime-status/historical-avg/{attractionId}
     */
    @GetMapping("/historical-avg/{attractionId}")
    public Result<Integer> getHistoricalAvgCrowdCount(@PathVariable Long attractionId) {
        try {
            log.info("获取景点历史人流均值请求: attractionId={}", attractionId);
            Integer avgCount = attractionRealtimeStatusService.selectAvgCrowdCount(attractionId);
            return Result.success("获取历史人流均值成功", avgCount);
        } catch (Exception e) {
            log.error("获取景点历史人流均值失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取需要同步的景点状态
     * GET /api/realtime-status/need-sync
     */
    @GetMapping("/need-sync")
    public Result<List<AttractionRealtimeStatus>> getNeedSyncStatus(
            @RequestParam(defaultValue = "60") Integer minutes) {
        try {
            log.info("获取需要同步的景点状态请求: minutes={}", minutes);
            List<AttractionRealtimeStatus> statusList = attractionRealtimeStatusService.selectNeedSyncStatus(minutes);
            return Result.success("获取需要同步的状态成功", statusList);
        } catch (Exception e) {
            log.error("获取需要同步的景点状态失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取活跃的预警信息
     * GET /api/realtime-status/warns
     */
    @GetMapping("/warns")
    public Result<List<Object>> getActiveWarns() {
        try {
            log.info("获取活跃的预警信息请求");
            List<Object> warns = attractionRealtimeStatusService.getActiveWarns();
            return Result.success("获取预警信息成功", warns);
        } catch (Exception e) {
            log.error("获取活跃的预警信息失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量更新同步时间
     * POST /api/realtime-status/sync-time
     */
    @PostMapping("/sync-time")
    public Result<Integer> batchUpdateSyncTime(@RequestBody Long[] attractionIds) {
        try {
            log.info("批量更新同步时间请求: count={}", attractionIds.length);
            int count = attractionRealtimeStatusService.batchUpdateSyncTime(attractionIds);
            return Result.success("批量更新同步时间成功", count);
        } catch (Exception e) {
            log.error("批量更新同步时间失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取景点近7天人流均值
     * GET /api/realtime-status/7days-avg/{attractionId}
     */
    @GetMapping("/7days-avg/{attractionId}")
    public Result<Integer> get7DaysAvgCrowdCount(@PathVariable Long attractionId) {
        try {
            log.info("获取景点近7天人流均值请求: attractionId={}", attractionId);
            Integer avgCount = attractionRealtimeStatusService.selectAvgCrowdCountUsingMapper(attractionId);
            return Result.success("获取近7天人流均值成功", avgCount);
        } catch (Exception e) {
            log.error("获取景点近7天人流均值失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
