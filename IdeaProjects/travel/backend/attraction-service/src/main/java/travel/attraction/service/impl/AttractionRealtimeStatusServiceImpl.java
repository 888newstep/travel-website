package travel.attraction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.attraction.service.AttractionRealtimeStatusService;
import travel.attraction.dto.AttractionWarning;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionRealtimeStatusServiceImpl extends ServiceImpl<AttractionRealtimeStatusMapper, AttractionRealtimeStatus> implements AttractionRealtimeStatusService {

    private final AttractionRealtimeStatusMapper attractionRealtimeStatusMapper;
    private final CacheUtil cacheUtil;
    
    // 缓存常量
    private static final String ATTRACTION_STATUS_PREFIX = "attraction:status:";
    private static final String ACTIVE_WARNS_PREFIX = "attraction:active_warns";
    private static final int CACHE_EXPIRE_MINUTES = 30;
    private static final int WARN_CACHE_EXPIRE_MINUTES = 5;
    private static final int MAX_WARNING_SOURCE_RECORDS = 500;
    private static final List<String> SEVERE_WEATHER_KEYWORDS = List.of(
            "暴雨", "大雨", "雷", "台风", "冰雹", "暴雪", "大雪", "大风", "沙尘", "浓雾");

    @Override
    public AttractionRealtimeStatus getByAttractionId(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 生成缓存键
        String cacheKey = ATTRACTION_STATUS_PREFIX + attractionId;
        
        // 尝试从缓存获取
        AttractionRealtimeStatus cachedStatus = cacheUtil.get(cacheKey, AttractionRealtimeStatus.class);
        if (cachedStatus != null) {
            log.info("从缓存获取景点实时状态: attractionId={}", attractionId);
            return cachedStatus;
        }

        LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AttractionRealtimeStatus::getAttractionId, attractionId)
                .eq(AttractionRealtimeStatus::getDeleted, 0);

        AttractionRealtimeStatus status = getOne(queryWrapper, false);
        if (status == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_STATUS_NOT_EXIST);
        }

        // 缓存结果
        cacheUtil.set(cacheKey, status, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<AttractionRealtimeStatus> statusList) {
        if (statusList == null || statusList.isEmpty()) {
            return false;
        }

        for (AttractionRealtimeStatus status : statusList) {
            if (status == null || status.getAttractionId() == null || status.getAttractionId() <= 0) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AttractionRealtimeStatus::getAttractionId, status.getAttractionId())
                    .eq(AttractionRealtimeStatus::getDeleted, 0);

            AttractionRealtimeStatus existingStatus = getOne(queryWrapper, false);
            status.setUpdateTime(LocalDateTime.now());
            boolean saved;
            if (existingStatus != null) {
                status.setId(existingStatus.getId());
                saved = updateById(status);
            } else {
                saved = save(status);
            }
            if (!saved) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_DATABASE_ERROR);
            }

            cacheUtil.delete(ATTRACTION_STATUS_PREFIX + status.getAttractionId());
            log.info("清除景点实时状态缓存: attractionId={}", status.getAttractionId());
        }
        cacheUtil.delete(ACTIVE_WARNS_PREFIX);
        return true;
    }

    @Override
    public List<AttractionRealtimeStatus> selectNeedSyncStatus(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(minutes);

        LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AttractionRealtimeStatus::getDeleted, 0)
                .and(wrapper -> wrapper
                        .isNull(AttractionRealtimeStatus::getUpdateTime)
                        .or()
                        .lt(AttractionRealtimeStatus::getUpdateTime, thresholdTime)
                );

        return list(queryWrapper);
    }

    /**
     * 更新或保存景点实时状态
     */
    public boolean updateOrSave(AttractionRealtimeStatus status) {
        if (status == null || status.getAttractionId() == null) {
            return false;
        }

        LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AttractionRealtimeStatus::getAttractionId, status.getAttractionId())
                .eq(AttractionRealtimeStatus::getDeleted, 0);

        AttractionRealtimeStatus existingStatus = getOne(queryWrapper, false);
        status.setUpdateTime(LocalDateTime.now());

        boolean result;
        if (existingStatus != null) {
            status.setId(existingStatus.getId());
            result = updateById(status);
        } else {
            result = save(status);
        }
        
        // 清除缓存
        if (result) {
            String cacheKey = ATTRACTION_STATUS_PREFIX + status.getAttractionId();
            cacheUtil.delete(cacheKey);
            cacheUtil.delete(ACTIVE_WARNS_PREFIX);
            log.info("清除景点实时状态缓存: attractionId={}", status.getAttractionId());
        }
        
        return result;
    }

    /**
     * 批量获取景点实时状态
     */
    public List<AttractionRealtimeStatus> getByAttractionIds(List<Long> attractionIds) {
        if (attractionIds == null || attractionIds.isEmpty()) {
            return List.of();
        }

        // 尝试从缓存获取每个景点的状态
        Map<Long, AttractionRealtimeStatus> statusesByAttractionId = new LinkedHashMap<>();
        List<Long> missingAttractionIds = new java.util.ArrayList<>();

        for (Long attractionId : attractionIds) {
            String cacheKey = ATTRACTION_STATUS_PREFIX + attractionId;
            AttractionRealtimeStatus cachedStatus = cacheUtil.get(cacheKey, AttractionRealtimeStatus.class);
            if (cachedStatus != null) {
                statusesByAttractionId.put(attractionId, cachedStatus);
            } else {
                missingAttractionIds.add(attractionId);
            }
        }

        // 如果所有景点状态都在缓存中，直接返回
        if (missingAttractionIds.isEmpty()) {
            log.info("从缓存批量获取景点实时状态: count={}", statusesByAttractionId.size());
            return orderStatuses(attractionIds, statusesByAttractionId);
        }

        // 从数据库查询缺失的景点状态
        LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(AttractionRealtimeStatus::getAttractionId, missingAttractionIds)
                .eq(AttractionRealtimeStatus::getDeleted, 0);

        List<AttractionRealtimeStatus> dbStatuses = list(queryWrapper);

        // 缓存查询结果并添加到返回列表
        for (AttractionRealtimeStatus status : dbStatuses) {
            String cacheKey = ATTRACTION_STATUS_PREFIX + status.getAttractionId();
            cacheUtil.set(cacheKey, status, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            statusesByAttractionId.put(status.getAttractionId(), status);
        }

        return orderStatuses(attractionIds, statusesByAttractionId);
    }

    /**
     * 获取活跃的预警信息
     */
    @SuppressWarnings("unchecked")
    public List<AttractionWarning> getActiveWarns() {
        // 生成缓存键
        String cacheKey = ACTIVE_WARNS_PREFIX;
        
        // 尝试从缓存获取
        List<AttractionWarning> cachedWarns = cacheUtil.get(cacheKey, List.class);
        if (cachedWarns != null) {
            log.info("从缓存获取活跃预警信息: count={}", cachedWarns.size());
            return cachedWarns;
        }

        LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AttractionRealtimeStatus::getDeleted, 0)
                .orderByDesc(AttractionRealtimeStatus::getUpdateTime)
                .last("LIMIT " + MAX_WARNING_SOURCE_RECORDS);
        List<AttractionWarning> activeWarns = new ArrayList<>();
        for (AttractionRealtimeStatus realtimeStatus : list(queryWrapper)) {
            addCrowdWarning(realtimeStatus, activeWarns);
            addWeatherWarning(realtimeStatus, activeWarns);
        }
        activeWarns.sort(Comparator
                .comparingInt((AttractionWarning warning) -> warningSeverity(warning.getWarnLevel()))
                .reversed()
                .thenComparing(AttractionWarning::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        cacheUtil.set(cacheKey, activeWarns, WARN_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("获取到 {} 条活跃的预警信息", activeWarns.size());
        return activeWarns;
    }

    /**
     * 批量更新同步时间
     * @param attractionIds 景点ID数组
     * @return 受影响行数
     */
    @Override
    public int batchUpdateSyncTime(Long[] attractionIds) {
        if (attractionIds == null || attractionIds.length == 0) {
            return 0;
        }
        // 使用attractionRealtimeStatusMapper批量更新同步时间
        int updated = attractionRealtimeStatusMapper.batchUpdateSyncTime(attractionIds);
        cacheUtil.delete(ACTIVE_WARNS_PREFIX);
        return updated;
    }

    private void addCrowdWarning(
            AttractionRealtimeStatus realtimeStatus, List<AttractionWarning> warnings) {
        Integer crowdLevel = realtimeStatus.getCrowdLevel();
        if (crowdLevel == null || crowdLevel < 4) {
            return;
        }
        AttractionWarning warning = baseWarning(realtimeStatus, "crowd");
        warning.setWarnType("人流预警");
        warning.setWarnLevel(crowdLevel >= 5 ? "严重" : "较高");
        warning.setWarnMessage(crowdLevel >= 5
                ? "当前景点人流已达到最高预警等级，请暂缓前往"
                : "当前景点人流较为拥挤，建议错峰游览");
        warnings.add(warning);
    }

    private void addWeatherWarning(
            AttractionRealtimeStatus realtimeStatus, List<AttractionWarning> warnings) {
        String weather = realtimeStatus.getWeather();
        if (weather == null || weather.isBlank()) {
            return;
        }
        String normalizedWeather = weather.toLowerCase(Locale.ROOT);
        if (SEVERE_WEATHER_KEYWORDS.stream().noneMatch(normalizedWeather::contains)) {
            return;
        }
        AttractionWarning warning = baseWarning(realtimeStatus, "weather");
        warning.setWarnType("天气预警");
        warning.setWarnLevel("较高");
        warning.setWarnMessage("当前景点天气为" + weather + "，请关注安全并合理调整行程");
        warnings.add(warning);
    }

    private AttractionWarning baseWarning(AttractionRealtimeStatus realtimeStatus, String type) {
        AttractionWarning warning = new AttractionWarning();
        warning.setWarnId(type + ":" + realtimeStatus.getAttractionId());
        warning.setAttractionId(realtimeStatus.getAttractionId());
        warning.setCreateTime(realtimeStatus.getUpdateTime());
        warning.setStatus("active");
        return warning;
    }

    private int warningSeverity(String warnLevel) {
        return "严重".equals(warnLevel) ? 3 : "较高".equals(warnLevel) ? 2 : 1;
    }

    private List<AttractionRealtimeStatus> orderStatuses(
            List<Long> attractionIds, Map<Long, AttractionRealtimeStatus> statusesByAttractionId) {
        return attractionIds.stream()
                .map(statusesByAttractionId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
