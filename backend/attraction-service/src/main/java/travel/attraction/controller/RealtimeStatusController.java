package travel.attraction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.attraction.service.AttractionRealtimeStatusService;
import travel.attraction.service.RealtimeTrafficService;
import travel.attraction.dto.AttractionWarning;
import travel.common.utils.Result;
import travel.common.security.AuthenticatedUserSupport;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/realtime-status")
@RequiredArgsConstructor
@Tag(name = "景点实时状态", description = "景点客流状态与高德天气同步接口")
public class RealtimeStatusController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeStatusController.class);
    private static final int MAX_BATCH_SIZE = 100;

    private final AttractionRealtimeStatusService attractionRealtimeStatusService;
    private final RealtimeTrafficService realtimeTrafficService;

    /**
     * 获取景点实时状态
     * GET /api/realtime-status/attraction/{attractionId}
     */
    @GetMapping("/attraction/{attractionId}")
    public Result<AttractionRealtimeStatus> getAttractionRealtimeStatus(@PathVariable Long attractionId) {
        try {
            validateAttractionId(attractionId);
            log.info("获取景点实时状态请求: attractionId={}", attractionId);
            AttractionRealtimeStatus status = attractionRealtimeStatusService.getByAttractionId(attractionId);
            return Result.success("获取实时状态成功", status);
        } catch (Exception e) {
            log.error("获取景点实时状态失败: attractionId={}, error={}", attractionId, e.getMessage());
            throw propagate(e, "获取景点实时状态失败");
        }
    }

    /**
     * 批量获取景点实时状态
     * POST /api/realtime-status/batch
     */
    @PostMapping("/batch")
    public Result<List<AttractionRealtimeStatus>> getBatchRealtimeStatus(@RequestBody List<Long> attractionIds) {
        try {
            validateIdBatch(attractionIds);
            log.info("批量获取景点实时状态请求: attractionIds={}", attractionIds);
            List<AttractionRealtimeStatus> statusList = attractionRealtimeStatusService.getByAttractionIds(attractionIds);
            return Result.success("批量获取实时状态成功", statusList);
        } catch (Exception e) {
            log.error("批量获取景点实时状态失败: error={}", e.getMessage());
            throw propagate(e, "批量获取景点实时状态失败");
        }
    }

    /**
     * 更新或保存景点实时状态
     * POST /api/realtime-status/update
     */
    @PostMapping("/update")
    public Result<Boolean> updateRealtimeStatus(@RequestBody AttractionRealtimeStatus status) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            validateStatus(status);
            log.info("更新景点实时状态请求: attractionId={}", status.getAttractionId());
            boolean result = attractionRealtimeStatusService.updateOrSave(status);
            return Result.success("更新实时状态成功", result);
        } catch (Exception e) {
            log.error("更新景点实时状态失败: error={}", e.getMessage());
            throw propagate(e, "更新景点实时状态失败");
        }
    }

    /**
     * 批量更新景点实时状态
     * POST /api/realtime-status/batch-update
     */
    @PostMapping("/batch-update")
    public Result<Boolean> batchUpdateRealtimeStatus(@RequestBody List<AttractionRealtimeStatus> statusList) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            validateBatch(statusList);
            statusList.forEach(this::validateStatus);
            log.info("批量更新景点实时状态请求: count={}", statusList.size());
            boolean result = attractionRealtimeStatusService.batchUpdateStatus(statusList);
            return Result.success("批量更新实时状态成功", result);
        } catch (Exception e) {
            log.error("批量更新景点实时状态失败: error={}", e.getMessage());
            throw propagate(e, "批量更新景点实时状态失败");
        }
    }

    /**
     * 获取景点历史人流均值
     * GET /api/realtime-status/historical-avg/{attractionId}
     */
    @GetMapping("/historical-avg/{attractionId}")
    public Result<Integer> getHistoricalAvgCrowdCount(@PathVariable Long attractionId) {
        validateAttractionId(attractionId);
        throw new BusinessException(ErrorCodeEnum.REALTIME_HISTORY_UNAVAILABLE);
    }

    /**
     * 获取需要同步的景点状态
     * GET /api/realtime-status/need-sync
     */
    @GetMapping("/need-sync")
    public Result<List<AttractionRealtimeStatus>> getNeedSyncStatus(
            @RequestParam(defaultValue = "60") Integer minutes) {
        try {
            if (minutes == null || minutes <= 0 || minutes > 10_080) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            log.info("获取需要同步的景点状态请求: minutes={}", minutes);
            List<AttractionRealtimeStatus> statusList = attractionRealtimeStatusService.selectNeedSyncStatus(minutes);
            return Result.success("获取需要同步的状态成功", statusList);
        } catch (Exception e) {
            log.error("获取需要同步的景点状态失败: error={}", e.getMessage());
            throw propagate(e, "获取待同步景点状态失败");
        }
    }

    /**
     * 获取活跃的预警信息
     * GET /api/realtime-status/warns
     */
    @GetMapping("/warns")
    public Result<List<AttractionWarning>> getActiveWarns() {
        try {
            log.info("获取活跃的预警信息请求");
            List<AttractionWarning> warns = attractionRealtimeStatusService.getActiveWarns();
            return Result.success("获取预警信息成功", warns);
        } catch (Exception e) {
            log.error("获取活跃的预警信息失败: error={}", e.getMessage());
            throw propagate(e, "获取活跃预警信息失败");
        }
    }

    /**
     * 批量更新同步时间
     * POST /api/realtime-status/sync-time
     */
    @PostMapping("/sync-time")
    public Result<Integer> batchUpdateSyncTime(@RequestBody Long[] attractionIds) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            if (attractionIds == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            validateIdBatch(java.util.Arrays.asList(attractionIds));
            log.info("批量更新同步时间请求: count={}", attractionIds.length);
            int count = attractionRealtimeStatusService.batchUpdateSyncTime(attractionIds);
            return Result.success("批量更新同步时间成功", count);
        } catch (Exception e) {
            log.error("批量更新同步时间失败: error={}", e.getMessage());
            throw propagate(e, "批量更新同步时间失败");
        }
    }

    /**
     * 获取景点近7天人流均值
     * GET /api/realtime-status/7days-avg/{attractionId}
     */
    @GetMapping("/7days-avg/{attractionId}")
    public Result<Integer> get7DaysAvgCrowdCount(@PathVariable Long attractionId) {
        validateAttractionId(attractionId);
        throw new BusinessException(ErrorCodeEnum.REALTIME_HISTORY_UNAVAILABLE);
    }

    // ==================== 景点实时状态同步接口 ====================

    @GetMapping("/crowded")
    @Operation(summary = "获取拥挤景点列表")
    public Result<List<AttractionRealtimeStatus>> getCrowdedAttractions(@RequestParam(defaultValue = "3") int minCrowdLevel) {
        try {
            if (minCrowdLevel < 1 || minCrowdLevel > 5) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            log.info("获取拥挤景点列表请求: minCrowdLevel={}", minCrowdLevel);
            List<AttractionRealtimeStatus> crowdedAttractions =
                    realtimeTrafficService.getCrowdedAttractions(minCrowdLevel);
            return Result.success("获取成功", crowdedAttractions);
        } catch (Exception e) {
            log.error("获取拥挤景点列表失败: error={}", e.getMessage());
            throw propagate(e, "获取拥挤景点列表失败");
        }
    }

    @PostMapping("/traffic-update")
    @Operation(summary = "更新景点实时状态")
    public Result<Void> updateAttractionStatus(@RequestBody Map<String, Object> params) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            Long attractionId = requirePositiveLong(params, "attractionId");
            Double longitude = getCoordinate(params, "longitude", -180, 180);
            Double latitude = getCoordinate(params, "latitude", -90, 90);
            validateCoordinatePair(longitude, latitude);

            log.info("更新景点实时状态请求: attractionId={}", attractionId);
            realtimeTrafficService.updateAttractionRealtimeStatus(attractionId, longitude, latitude);

            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新景点状态失败: error={}", e.getMessage());
            throw propagate(e, "更新景点状态失败");
        }
    }

    @PostMapping("/traffic-batch")
    @Operation(summary = "批量更新景点实时状态")
    public Result<Void> batchUpdateAttractionStatus(@RequestBody List<Map<String, Object>> attractions) {
        AuthenticatedUserSupport.requireAdmin();
        try {
            validateBatch(attractions);
            for (Map<String, Object> attraction : attractions) {
                requirePositiveLong(attraction, "id");
                Double longitude = getCoordinate(attraction, "longitude", -180, 180);
                Double latitude = getCoordinate(attraction, "latitude", -90, 90);
                validateCoordinatePair(longitude, latitude);
            }
            log.info("批量更新景点实时状态请求: count={}", attractions.size());
            realtimeTrafficService.batchUpdateAttractionsRealtimeStatus(attractions);
            return Result.success("批量更新成功", null);
        } catch (Exception e) {
            log.error("批量更新景点状态失败: error={}", e.getMessage());
            throw propagate(e, "批量更新景点状态失败");
        }
    }

    private void validateStatus(AttractionRealtimeStatus status) {
        if (status == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        validateAttractionId(status.getAttractionId());
        if (status.getCrowdCount() != null && status.getCrowdCount() < 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (status.getCrowdLevel() != null
                && (status.getCrowdLevel() < 1 || status.getCrowdLevel() > 5)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (status.getTemperature() != null
                && (status.getTemperature() < -100 || status.getTemperature() > 100)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateAttractionId(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateIdBatch(List<Long> attractionIds) {
        validateBatch(attractionIds);
        if (attractionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateBatch(List<?> items) {
        if (items == null || items.isEmpty() || items.size() > MAX_BATCH_SIZE || items.contains(null)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private Long requirePositiveLong(Map<String, Object> params, String key) {
        if (params == null || !(params.get(key) instanceof Number value)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        long result = value.longValue();
        if (result <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return result;
    }

    private Double getCoordinate(Map<String, Object> params, String key, double min, double max) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        if (!(params.get(key) instanceof Number value)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        double result = value.doubleValue();
        if (!Double.isFinite(result) || result < min || result > max) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return result;
    }

    private void validateCoordinatePair(Double longitude, Double latitude) {
        if ((longitude == null) != (latitude == null)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private RuntimeException propagate(Exception exception, String operation) {
        if (exception instanceof BusinessException businessException) {
            return businessException;
        }
        return new RuntimeException(operation, exception);
    }
}
