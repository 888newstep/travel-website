package travel.route.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import travel.common.dto.route.RouteVisitDailyAggregate;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteVisit;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.route_planning_mapper.RouteVisitMapper;
import travel.route.dto.route.RouteVisitAnalyticsResponse;
import travel.route.dto.route.RouteVisitTrendItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteVisitAnalyticsService {

    private final RouteMapper routeMapper;
    private final RouteVisitMapper routeVisitMapper;
    private final RouteCacheService routeCacheService;

    @Value("${travel.route.analytics.hash-salt}")
    private String hashSalt;

    @Transactional
    public void recordVisit(Route route, Integer userId, HttpServletRequest request) {
        if (route == null || route.getId() == null || request == null) {
            throw new IllegalArgumentException("route and request are required for visit analytics");
        }
        if (hashSalt == null || hashSalt.isBlank()) {
            throw new IllegalStateException("route analytics hash salt is not configured");
        }

        LocalDateTime now = LocalDateTime.now();
        RouteVisit visit = new RouteVisit();
        visit.setRouteId(route.getId());
        visit.setUserId(userId);
        visit.setVisitorType(userId == null ? "ANONYMOUS" : "AUTHENTICATED");
        visit.setVisitorHash(visitorHash(userId, request));
        visit.setVisitDate(now.toLocalDate());
        visit.setVisitedAt(now);

        if (routeMapper.incrementViewCount(route.getId()) != 1) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        if (routeVisitMapper.insert(visit) != 1) {
            throw new IllegalStateException("route visit detail was not persisted");
        }
        invalidateRouteCacheAfterCommit(route.getId());
    }

    public RouteVisitAnalyticsResponse getAnalytics(Integer routeId, int days) {
        if (routeId == null || routeId <= 0 || days < 1 || days > 365) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        long periodVisits = routeVisitMapper.countVisits(routeId, startDate, endDate);
        long uniqueVisitors = routeVisitMapper.countUniqueVisitors(routeId, startDate, endDate);
        long returningVisitors = routeVisitMapper.countReturningVisitors(routeId, startDate, endDate);
        double retentionRate = uniqueVisitors == 0
                ? 0.0
                : Math.round(returningVisitors * 10_000.0 / uniqueVisitors) / 10_000.0;

        Map<LocalDate, RouteVisitDailyAggregate> aggregates = new HashMap<>();
        for (RouteVisitDailyAggregate aggregate :
                routeVisitMapper.selectDailyTrend(routeId, startDate, endDate)) {
            aggregates.put(aggregate.getVisitDate(), aggregate);
        }
        List<RouteVisitTrendItem> trend = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    RouteVisitDailyAggregate aggregate = aggregates.get(date);
                    return aggregate == null
                            ? new RouteVisitTrendItem(date, 0, 0)
                            : new RouteVisitTrendItem(
                                    date,
                                    valueOrZero(aggregate.getVisits()),
                                    valueOrZero(aggregate.getUniqueVisitors()));
                })
                .toList();

        return new RouteVisitAnalyticsResponse(
                routeId,
                startDate,
                endDate,
                route.getViewCount() == null ? 0 : route.getViewCount(),
                periodVisits,
                uniqueVisitors,
                returningVisitors,
                retentionRate,
                trend);
    }

    private String visitorHash(Integer userId, HttpServletRequest request) {
        String actor;
        if (userId != null) {
            actor = "user:" + userId;
        } else {
            String remoteAddress = safeText(request.getRemoteAddr(), "unknown-address");
            String userAgent = safeText(request.getHeader("User-Agent"), "unknown-agent");
            actor = "anonymous:" + remoteAddress + '\u0000' + userAgent;
        }
        return sha256(hashSalt + '\u0000' + actor);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private void invalidateRouteCacheAfterCommit(Integer routeId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidateRouteCacheSafely(routeId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidateRouteCacheSafely(routeId);
            }
        });
    }

    private void invalidateRouteCacheSafely(Integer routeId) {
        try {
            routeCacheService.invalidateRouteCache(routeId);
        } catch (RuntimeException exception) {
            log.warn("路线访问已入库，但详情缓存失效失败: routeId={}, error={}",
                    routeId, exception.getMessage());
        }
    }
}
