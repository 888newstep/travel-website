package travel.controller.travel_realtime_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.service.travel_realtime.RealtimeTrafficService;
import travel.utils.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/realtime-status")
@Tag(name = "实时交通", description = "实时交通和景点状态相关接口")
public class RealtimeTrafficController {

    @Autowired
    private RealtimeTrafficService realtimeTrafficService;

    @GetMapping("/attraction/{attractionId}")
    @Operation(summary = "获取景点实时状态")
    public Result<AttractionRealtimeStatus> getAttractionRealtimeStatus(@PathVariable Long attractionId) {
        try {
            AttractionRealtimeStatus status = realtimeTrafficService.getAttractionRealtimeStatus(attractionId);
            return Result.success("获取成功", status);
        } catch (Exception e) {
            return Result.error("获取景点实时状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/crowded")
    @Operation(summary = "获取拥挤景点列表")
    public Result<List<AttractionRealtimeStatus>> getCrowdedAttractions(@RequestParam(defaultValue = "3") int minCrowdLevel) {
        try {
            List<AttractionRealtimeStatus> crowdedAttractions =
                    realtimeTrafficService.getCrowdedAttractions(minCrowdLevel);
            return Result.success("获取成功", crowdedAttractions);
        } catch (Exception e) {
            return Result.error("获取拥挤景点列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    @Operation(summary = "更新景点实时状态")
    public Result<Void> updateAttractionStatus(@RequestBody Map<String, Object> params) {
        try {
            Long attractionId = ((Number) params.get("attractionId")).longValue();
            Double longitude = (Double) params.get("longitude");
            Double latitude = (Double) params.get("latitude");

            realtimeTrafficService.updateAttractionRealtimeStatus(attractionId, longitude, latitude);

            return Result.success("更新成功", null);
        } catch (Exception e) {
            return Result.error("更新景点状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch")
    @Operation(summary = "批量更新景点实时状态")
    public Result<Void> batchUpdateAttractionStatus(@RequestBody List<Map<String, Object>> attractions) {
        try {
            realtimeTrafficService.batchUpdateAttractionsRealtimeStatus(attractions);
            return Result.success("批量更新成功", null);
        } catch (Exception e) {
            return Result.error("批量更新景点状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/historical-avg/{attractionId}")
    @Operation(summary = "获取历史平均人流")
    public Result<Integer> getHistoricalAvgCrowdCount(@PathVariable Long attractionId) {
        try {
            Integer avgCount = realtimeTrafficService.getHistoricalAvgCrowdCount(attractionId);
            return Result.success("获取成功", avgCount);
        } catch (Exception e) {
            return Result.error("获取历史平均人流失败: " + e.getMessage());
        }
    }

    @GetMapping("/need-sync")
    @Operation(summary = "获取需要同步的景点状态")
    public Result<List<AttractionRealtimeStatus>> getNeedSyncStatus(@RequestParam(defaultValue = "60") int minutes) {
        try {
            List<AttractionRealtimeStatus> needSyncList = realtimeTrafficService.getNeedSyncStatus(minutes);
            return Result.success("获取成功", needSyncList);
        } catch (Exception e) {
            return Result.error("获取需要同步的景点状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/sync-time")
    @Operation(summary = "批量更新同步时间")
    public Result<Integer> batchUpdateSyncTime(@RequestBody List<Long> attractionIds) {
        try {
            int count = realtimeTrafficService.batchUpdateSyncTime(attractionIds);
            return Result.success("更新成功", count);
        } catch (Exception e) {
            return Result.error("批量更新同步时间失败: " + e.getMessage());
        }
    }

    @GetMapping("/7days-avg/{attractionId}")
    @Operation(summary = "获取7天平均人流")
    public Result<Integer> get7DaysAvgCrowdCount(@PathVariable Long attractionId) {
        try {
            Integer avgCount = realtimeTrafficService.get7DaysAvgCrowdCount(attractionId);
            return Result.success("获取成功", avgCount);
        } catch (Exception e) {
            return Result.error("获取7天平均人流失败: " + e.getMessage());
        }
    }

    @GetMapping("/warns")
    @Operation(summary = "获取活跃预警")
    public Result<List<?>> getActiveWarns() {
        try {
            List<?> warns = realtimeTrafficService.getActiveWarns();
            return Result.success("获取成功", warns);
        } catch (Exception e) {
            return Result.error("获取活跃预警失败: " + e.getMessage());
        }
    }
}
