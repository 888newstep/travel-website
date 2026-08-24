package travel.attraction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travel.common.enums.ErrorCodeEnum;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.AMapService;
import travel.common.utils.CacheUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeTrafficService {

    private final AttractionRealtimeStatusMapper realtimeStatusMapper;
    private final AMapService aMapService;
    private final CacheUtil cacheUtil;

    /**
     * 更新景点实时状态
     */
    public void updateAttractionRealtimeStatus(Long attractionId, Double longitude, Double latitude) {
        AttractionRealtimeStatus existing = realtimeStatusMapper.selectByAttractionId(attractionId);
        AttractionRealtimeStatus status = new AttractionRealtimeStatus();
        status.setAttractionId(attractionId);

        if (longitude == null || latitude == null
                || longitude < -180 || longitude > 180
                || latitude < -90 || latitude > 90) {
            throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_FETCH_FAILED);
        }
        String location = longitude + "," + latitude;
        Map<String, Object> weather = aMapService.getWeatherByLocation(location);
        applyWeather(status, weather);

        if (status.getWeather() == null && status.getTemperature() == null) {
            throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_FETCH_FAILED);
        }

        // 当前仅接入真实天气数据；人流供应商接入前保留已有值，禁止写入模拟数据。
        if (existing != null) {
            status.setCrowdCount(existing.getCrowdCount());
            status.setCrowdLevel(existing.getCrowdLevel());
        }

        status.setUpdateTime(LocalDateTime.now());
        status.setDeleted(0);

        // 检查是否已存在记录
        if (existing != null) {
            status.setId(existing.getId());
            realtimeStatusMapper.updateById(status);
        } else {
            realtimeStatusMapper.insert(status);
        }

        cacheUtil.delete("attraction:status:" + attractionId);
        cacheUtil.delete("attraction:active_warns");

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
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateAttractionsRealtimeStatus(List<Map<String, Object>> attractions) {
        for (Map<String, Object> attraction : attractions) {
            Long attractionId = ((Number) attraction.get("id")).longValue();
            Double longitude = toDouble(attraction.get("longitude"));
            Double latitude = toDouble(attraction.get("latitude"));
            updateAttractionRealtimeStatus(attractionId, longitude, latitude);
        }

    }

    private void applyWeather(AttractionRealtimeStatus status, Map<String, Object> weather) {
        if (weather == null) {
            return;
        }
        Object weatherValue = weather.get("weather");
        Object temperatureValue = weather.get("temperature");
        if (weatherValue != null) {
            status.setWeather(weatherValue.toString());
        }
        if (temperatureValue instanceof Number number) {
            status.setTemperature(number.intValue());
        } else if (temperatureValue != null) {
            try {
                status.setTemperature(Integer.valueOf(temperatureValue.toString()));
            } catch (NumberFormatException exception) {
                log.warn("忽略无法解析的天气温度: value={}", temperatureValue);
            }
        }
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    /**
     * 获取拥挤景点列表
     */
    public List<AttractionRealtimeStatus> getCrowdedAttractions(int minCrowdLevel) {
        return realtimeStatusMapper.selectByCrowdLevel(minCrowdLevel);
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

}
