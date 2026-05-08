package travel.controller.travel_realtime_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.service.travel_realtime.RealtimeTrafficService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/realtime")
@Tag(name = "实时交通", description = "实时交通和景点状态相关接口")
public class RealtimeTrafficController {

    @Autowired
    private RealtimeTrafficService realtimeTrafficService;

    @GetMapping("/attraction/{attractionId}")
    @Operation(summary = "获取景点实时状态")
    public Map<String, Object> getAttractionRealtimeStatus(@PathVariable Long attractionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            AttractionRealtimeStatus status = realtimeTrafficService.getAttractionRealtimeStatus(attractionId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", status);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取景点实时状态失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/crowded")
    @Operation(summary = "获取拥挤景点列表")
    public Map<String, Object> getCrowdedAttractions(@RequestParam(defaultValue = "3") int minCrowdLevel) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AttractionRealtimeStatus> crowdedAttractions =
                    realtimeTrafficService.getCrowdedAttractions(minCrowdLevel);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", crowdedAttractions);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取拥挤景点列表失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/attraction/update")
    @Operation(summary = "更新景点实时状态")
    public Map<String, Object> updateAttractionStatus(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long attractionId = ((Number) params.get("attractionId")).longValue();
            Double longitude = (Double) params.get("longitude");
            Double latitude = (Double) params.get("latitude");

            realtimeTrafficService.updateAttractionRealtimeStatus(attractionId, longitude, latitude);

            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新景点状态失败: " + e.getMessage());
        }
        return result;
    }
}
