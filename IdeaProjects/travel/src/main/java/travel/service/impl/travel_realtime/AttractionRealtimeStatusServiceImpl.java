package travel.service.impl.travel_realtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.service.travel_realtime.AttractionRealtimeStatusService;
import travel.utils.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionRealtimeStatusServiceImpl extends ServiceImpl<AttractionRealtimeStatusMapper, AttractionRealtimeStatus> implements AttractionRealtimeStatusService {

    private final AttractionRealtimeStatusMapper attractionRealtimeStatusMapper;
    private final CacheUtil cacheUtil;
    
    // 缓存常量
    private static final String ATTRACTION_STATUS_PREFIX = "attraction:status:";
    private static final String ATTRACTION_AVG_CROWD_PREFIX = "attraction:avg_crowd:";
    private static final String ACTIVE_WARNS_PREFIX = "attraction:active_warns";
    private static final int CACHE_EXPIRE_MINUTES = 30;

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

        AttractionRealtimeStatus status = getOne(queryWrapper);
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

        boolean success = true;
        for (AttractionRealtimeStatus status : statusList) {
            try {
                // 先查询是否存在
                LambdaQueryWrapper<AttractionRealtimeStatus> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(AttractionRealtimeStatus::getAttractionId, status.getAttractionId())
                        .eq(AttractionRealtimeStatus::getDeleted, 0);

                AttractionRealtimeStatus existingStatus = getOne(queryWrapper);
                if (existingStatus != null) {
                    // 更新
                    status.setId(existingStatus.getId());
                    status.setUpdateTime(LocalDateTime.now());
                    updateById(status);
                } else {
                    // 新增
                    status.setUpdateTime(LocalDateTime.now());
                    save(status);
                }
                
                // 清除缓存
                String cacheKey = ATTRACTION_STATUS_PREFIX + status.getAttractionId();
                cacheUtil.delete(cacheKey);
                log.info("清除景点实时状态缓存: attractionId={}", status.getAttractionId());
            } catch (Exception e) {
                log.error("批量更新景点实时状态失败: {}", e.getMessage(), e);
                success = false;
            }
        }

        return success;
    }

    @Override
    public Integer selectAvgCrowdCount(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 生成缓存键
        String cacheKey = ATTRACTION_AVG_CROWD_PREFIX + attractionId;
        
        // 尝试从缓存获取
        Integer cachedAvgCrowdCount = cacheUtil.get(cacheKey, Integer.class);
        if (cachedAvgCrowdCount != null) {
            log.info("从缓存获取景点历史人流均值: attractionId={}", attractionId);
            return cachedAvgCrowdCount;
        }

        try {
            // 使用mapper查询历史人流均值
            Integer avgCrowdCount = attractionRealtimeStatusMapper.selectAvgCrowdCount(attractionId);
            if (avgCrowdCount != null) {
                // 缓存结果
                cacheUtil.set(cacheKey, avgCrowdCount, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return avgCrowdCount;
            }
            
            // 如果查询结果为null，返回默认值
            log.warn("查询景点 {} 历史人流均值为null，返回默认值", attractionId);
            return 500;
        } catch (Exception e) {
            log.error("查询景点 {} 历史人流均值失败: {}", attractionId, e.getMessage(), e);
            // 异常情况下返回默认值
            return 500;
        }
    }

    @Override
    public List<AttractionRealtimeStatus> selectNeedSyncStatus(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            minutes = 60; // 默认1小时
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

        AttractionRealtimeStatus existingStatus = getOne(queryWrapper);
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
        List<AttractionRealtimeStatus> result = new java.util.ArrayList<>();
        List<Long> missingAttractionIds = new java.util.ArrayList<>();

        for (Long attractionId : attractionIds) {
            String cacheKey = ATTRACTION_STATUS_PREFIX + attractionId;
            AttractionRealtimeStatus cachedStatus = cacheUtil.get(cacheKey, AttractionRealtimeStatus.class);
            if (cachedStatus != null) {
                result.add(cachedStatus);
            } else {
                missingAttractionIds.add(attractionId);
            }
        }

        // 如果所有景点状态都在缓存中，直接返回
        if (missingAttractionIds.isEmpty()) {
            log.info("从缓存批量获取景点实时状态: count={}", result.size());
            return result;
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
            result.add(status);
        }

        return result;
    }

    /**
     * 获取活跃的预警信息
     */
    @SuppressWarnings("unchecked")
    public List<Object> getActiveWarns() {
        // 生成缓存键
        String cacheKey = ACTIVE_WARNS_PREFIX;
        
        // 尝试从缓存获取
        List<Object> cachedWarns = cacheUtil.get(cacheKey, List.class);
        if (cachedWarns != null) {
            log.info("从缓存获取活跃预警信息: count={}", cachedWarns.size());
            return cachedWarns;
        }

        try {
            // 模拟查询活跃的预警信息
            List<Object> activeWarns = new java.util.ArrayList<>();
            
            // 假设从数据库查询到的预警信息
            // 这里使用模拟数据
            java.util.Map<String, Object> warn1 = new java.util.HashMap<>();
            warn1.put("warnId", 1);
            warn1.put("attractionId", 1001L);
            warn1.put("attractionName", "故宫博物院");
            warn1.put("warnType", "人流预警");
            warn1.put("warnLevel", "严重");
            warn1.put("warnMessage", "当前人流量已超过最大承载量的80%");
            warn1.put("createTime", java.time.LocalDateTime.now().minusHours(1));
            warn1.put("status", "active");
            activeWarns.add(warn1);
            
            java.util.Map<String, Object> warn2 = new java.util.HashMap<>();
            warn2.put("warnId", 2);
            warn2.put("attractionId", 1002L);
            warn2.put("attractionName", "长城");
            warn2.put("warnType", "天气预警");
            warn2.put("warnLevel", "中度");
            warn2.put("warnMessage", "未来2小时内可能有大雨");
            warn2.put("createTime", java.time.LocalDateTime.now().minusHours(2));
            warn2.put("status", "active");
            activeWarns.add(warn2);
            
            java.util.Map<String, Object> warn3 = new java.util.HashMap<>();
            warn3.put("warnId", 3);
            warn3.put("attractionId", 1003L);
            warn3.put("attractionName", "西湖");
            warn3.put("warnType", "交通预警");
            warn3.put("warnLevel", "轻度");
            warn3.put("warnMessage", "景区周边道路轻度拥堵");
            warn3.put("createTime", java.time.LocalDateTime.now().minusHours(3));
            warn3.put("status", "active");
            activeWarns.add(warn3);
            
            // 缓存结果
            cacheUtil.set(cacheKey, activeWarns, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("获取到 {} 条活跃的预警信息", activeWarns.size());
            return activeWarns;
        } catch (Exception e) {
            log.error("获取活跃预警信息失败: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
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
        return attractionRealtimeStatusMapper.batchUpdateSyncTime(attractionIds);
    }

    /**
     * 查询景点近7天人流均值（使用attractionRealtimeStatusMapper）
     * @param attractionId 景点ID
     * @return 人流均值
     */
    @Override
    public Integer selectAvgCrowdCountUsingMapper(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        // 使用attractionRealtimeStatusMapper的selectAvgCrowdCount方法查询
        return attractionRealtimeStatusMapper.selectAvgCrowdCount(attractionId);
    }
}
