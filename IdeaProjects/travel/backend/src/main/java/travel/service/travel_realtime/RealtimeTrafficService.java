package travel.service.travel_realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.utils.AMapService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeTrafficService {

    private final AttractionRealtimeStatusMapper realtimeStatusMapper;

    private final AMapService aMapService;

    /**
     * 更新景点实时状态
     */
    public void updateAttractionRealtimeStatus(Long attractionId, Double longitude, Double latitude) {
        AttractionRealtimeStatus status = new AttractionRealtimeStatus();
        status.setAttractionId(attractionId);

        // 获取实时天气信息（这里使用城市代码，实际应该根据景点位置获取）
        Map<String, Object> weather = aMapService.getWeather("110000"); // 北京示例
        if (weather != null) {
            status.setWeather((String) weather.get("dayweather"));
            status.setTemperature((Integer) weather.get("daytemp"));
        }

        // 模拟人流数据（实际应该从其他数据源获取）
        status.setCrowdCount((int) (Math.random() * 1000));
        status.setCrowdLevel((int) (Math.random() * 5) + 1);

        status.setUpdateTime(LocalDateTime.now());
        status.setDeleted(0);

        // 检查是否已存在记录
        AttractionRealtimeStatus existing = realtimeStatusMapper.selectByAttractionId(attractionId);
        if (existing != null) {
            status.setId(existing.getId());
            realtimeStatusMapper.updateById(status);
        } else {
            realtimeStatusMapper.insert(status);
        }

        log.info("更新景点实时状态成功: attractionId={}", attractionId);
    }

    /**
     * 获取景点实时状态
     */
    public AttractionRealtimeStatus getAttractionRealtimeStatus(Long attractionId) {
        return realtimeStatusMapper.selectByAttractionId(attractionId);
    }

    /**
     * 批量更新景点实时状态
     */
    public void batchUpdateAttractionsRealtimeStatus(List<Map<String, Object>> attractions) {
        for (Map<String, Object> attraction : attractions) {
            Long attractionId = ((Number) attraction.get("id")).longValue();
            Double longitude = (Double) attraction.get("longitude");
            Double latitude = (Double) attraction.get("latitude");
            updateAttractionRealtimeStatus(attractionId, longitude, latitude);
        }
    }

    /**
     * 获取拥挤景点列表
     */
    public List<AttractionRealtimeStatus> getCrowdedAttractions(int minCrowdLevel) {
        return realtimeStatusMapper.selectByCrowdLevel(minCrowdLevel);
    }
}
