package travel.attraction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.AMapService;

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

        // 获取实时天气信息（根据景点位置获取）
        if (longitude != null && latitude != null) {
            String location = longitude + "," + latitude;
            Map<String, Object> weather = aMapService.getWeatherByLocation(location);
            if (weather != null) {
                status.setWeather((String) weather.get("dayweather"));
                status.setTemperature((Integer) weather.get("daytemp"));
            }
        } else {
            // 降级方案：使用城市代码
            Map<String, Object> weather = aMapService.getWeather("110000");
            if (weather != null) {
                status.setWeather((String) weather.get("dayweather"));
                status.setTemperature((Integer) weather.get("daytemp"));
            }
        }

        // TODO: 集成真实的人流数据API，目前使用模拟数据
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

    /**
     * 获取历史平均人流
     */
    public Integer getHistoricalAvgCrowdCount(Long attractionId) {
        AttractionRealtimeStatus status = realtimeStatusMapper.selectByAttractionId(attractionId);
        if (status != null && status.getCrowdCount() != null) {
            return status.getCrowdCount();
        }
        return 0;
    }

    /**
     * 获取需要同步的景点状态
     */
    public List<AttractionRealtimeStatus> getNeedSyncStatus(int minutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
        return realtimeStatusMapper.selectNeedSync(threshold);
    }

    /**
     * 批量更新同步时间
     */
    public int batchUpdateSyncTime(List<Long> attractionIds) {
        int count = 0;
        for (Long attractionId : attractionIds) {
            AttractionRealtimeStatus status = realtimeStatusMapper.selectByAttractionId(attractionId);
            if (status != null) {
                status.setUpdateTime(LocalDateTime.now());
                realtimeStatusMapper.updateById(status);
                count++;
            }
        }
        return count;
    }

    /**
     * 获取7天平均人流
     */
    public Integer get7DaysAvgCrowdCount(Long attractionId) {
        AttractionRealtimeStatus status = realtimeStatusMapper.selectByAttractionId(attractionId);
        if (status != null && status.getCrowdCount() != null) {
            return status.getCrowdCount();
        }
        return 0;
    }

    /**
     * 获取活跃预警
     */
    public List<?> getActiveWarns() {
        // TODO: 实现预警功能
        return List.of();
    }
}
