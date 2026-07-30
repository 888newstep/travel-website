package travel.attraction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.attraction.service.AttractionRealtimeStatusService;
import travel.attraction.service.RealtimeTrafficService;
import travel.common.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/realtime-status")
@RequiredArgsConstructor
@Tag(name = "实时交通", description = "实时交通和景点状态相关接口")
public class RealtimeStatusController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeStatusController.class);

    private final AttractionRealtimeStatusService attractionRealtimeStatusService;
    private final RealtimeTrafficService realtimeTrafficService;

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

    // ==================== 实时交通相关接口 ====================

    @GetMapping("/crowded")
    @Operation(summary = "获取拥挤景点列表")
    public Result<List<AttractionRealtimeStatus>> getCrowdedAttractions(@RequestParam(defaultValue = "3") int minCrowdLevel) {
        try {
            log.info("获取拥挤景点列表请求: minCrowdLevel={}", minCrowdLevel);
            List<AttractionRealtimeStatus> crowdedAttractions =
                    realtimeTrafficService.getCrowdedAttractions(minCrowdLevel);
            return Result.success("获取成功", crowdedAttractions);
        } catch (Exception e) {
            log.error("获取拥挤景点列表失败: error={}", e.getMessage());
            return Result.error("获取拥挤景点列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/traffic-update")
    @Operation(summary = "更新景点实时状态")
    public Result<Void> updateAttractionStatus(@RequestBody Map<String, Object> params) {
        try {
            Long attractionId = ((Number) params.get("attractionId")).longValue();
            Double longitude = (Double) params.get("longitude");
            Double latitude = (Double) params.get("latitude");

            log.info("更新景点实时状态请求: attractionId={}", attractionId);
            realtimeTrafficService.updateAttractionRealtimeStatus(attractionId, longitude, latitude);

            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新景点状态失败: error={}", e.getMessage());
            return Result.error("更新景点状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/traffic-batch")
    @Operation(summary = "批量更新景点实时状态")
    public Result<Void> batchUpdateAttractionStatus(@RequestBody List<Map<String, Object>> attractions) {
        try {
            log.info("批量更新景点实时状态请求: count={}", attractions.size());
            realtimeTrafficService.batchUpdateAttractionsRealtimeStatus(attractions);
            return Result.success("批量更新成功", null);
        } catch (Exception e) {
            log.error("批量更新景点状态失败: error={}", e.getMessage());
            return Result.error("批量更新景点状态失败: " + e.getMessage());
        }
    }
}
