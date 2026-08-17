package travel.route.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import travel.common.utils.CacheUtil;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * XXL-Job 定时任务处理器
 * 包含：景点热度统计、Redis缓存刷新、过期数据清理
 */
@Component
@RequiredArgsConstructor
public class TravelJobHandler {

    private static final Logger log = LoggerFactory.getLogger(TravelJobHandler.class);

    private final CacheUtil cacheUtil;

    /**
     * 定时统计景点热度（每30分钟执行一次）
     * Cron表达式: 0 0/30 * * * ?
     */
    @XxlJob("refreshAttractionHeat")
    public void refreshAttractionHeat() {
        log.info("XXL-Job: 开始刷新景点热度统计...");
        try {
            // 清除景点热度缓存，触发重新计算
            cacheUtil.deleteByPattern("attraction:heat:*");
            cacheUtil.set("attraction:heat:lastUpdate", LocalDateTime.now().toString(), 30, TimeUnit.MINUTES);
            log.info("XXL-Job: 景点热度刷新完成");
        } catch (Exception e) {
            log.error("XXL-Job: 景点热度刷新失败", e);
        }
    }

    /**
     * 定时刷新Redis热门路线缓存（每小时执行一次）
     * Cron: 0 0 * * * ?
     */
    @XxlJob("refreshPopularRoutesCache")
    public void refreshPopularRoutesCache() {
        log.info("XXL-Job: 开始刷新热门路线缓存...");
        try {
            cacheUtil.deleteByPattern("route:popular:*");
            cacheUtil.deleteByPattern("route:recommendation:*");
            log.info("XXL-Job: 热门路线缓存刷新完成");
        } catch (Exception e) {
            log.error("XXL-Job: 热门路线缓存刷新失败", e);
        }
    }

    /**
     * 定时清理过期数据（每天凌晨2点执行）
     * Cron: 0 0 2 * * ?
     */
    @XxlJob("cleanExpiredData")
    public void cleanExpiredData() {
        log.info("XXL-Job: 开始清理过期数据...");
        try {
            // 清理过期分享
            cacheUtil.deleteByPattern("share:expired:*");
            // 清理过期缓存
            cacheUtil.deleteByPattern("ai:cache:expired:*");
            log.info("XXL-Job: 过期数据清理完成");
        } catch (Exception e) {
            log.error("XXL-Job: 过期数据清理失败", e);
        }
    }

    /**
     * 定时同步路线数据（每15分钟执行一次）
     * Cron表达式: 0 0/15 * * * ?
     */
    @XxlJob("syncRouteData")
    public void syncRouteData() {
        log.info("XXL-Job: 开始同步路线数据...");
        try {
            cacheUtil.deleteByPattern("route:sync:*");
            cacheUtil.set("route:sync:lastSync", LocalDateTime.now().toString(), 15, TimeUnit.MINUTES);
            log.info("XXL-Job: 路线数据同步完成");
        } catch (Exception e) {
            log.error("XXL-Job: 路线数据同步失败", e);
        }
    }
}