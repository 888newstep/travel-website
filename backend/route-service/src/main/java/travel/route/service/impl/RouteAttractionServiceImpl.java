package travel.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.common.dto.request.RouteAttractionBatchSortRequest;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteAttractionMapper;
import travel.route.service.RouteAttractionService;
import travel.route.service.RouteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 路线-景点关联服务实现类
 */
@Service
@RequiredArgsConstructor
public class RouteAttractionServiceImpl extends ServiceImpl<RouteAttractionMapper, RouteAttraction> implements RouteAttractionService {

    private static final Logger log = LoggerFactory.getLogger(RouteAttractionServiceImpl.class);

    private final RouteAttractionMapper routeAttractionMapper;
    private final RouteService routeService;

    @Override
    public List<Long> getRouteIdsByAttractionId(Long attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteAttraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteAttraction::getAttractionId, attractionId.intValue());
        List<RouteAttraction> routeAttractions = list(queryWrapper);
        return routeAttractions.stream()
                .map(ra -> ra.getRouteId().longValue())
                .distinct() // 去重
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSortRouteAttractions(RouteAttractionBatchSortRequest request) {
        if (request == null || request.getRouteId() == null || request.getRouteId() <= 0
                || request.getRouteId() > Integer.MAX_VALUE
                || request.getSortItems() == null || request.getSortItems().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        if (routeService.getById(request.getRouteId()) == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        Integer routeId = request.getRouteId().intValue();
        List<RouteAttraction> currentSchedule =
                getByRouteIdOrderByDayAndVisitForUpdate(request.getRouteId());
        if (currentSchedule.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_RELATION_NOT_EXIST);
        }

        Map<Long, RouteAttraction> relationsById = new HashMap<>();
        for (RouteAttraction relation : currentSchedule) {
            relationsById.put(relation.getId().longValue(), relation);
        }

        Set<Long> requestedRelationIds = new HashSet<>();
        boolean changed = false;
        for (RouteAttractionBatchSortRequest.SortItem item : request.getSortItems()) {
            if (item == null || item.getRelationId() == null || item.getRelationId() <= 0
                    || item.getDayNumber() == null || item.getDayNumber() <= 0
                    || item.getVisitOrder() == null || item.getVisitOrder() <= 0) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            if (!requestedRelationIds.add(item.getRelationId())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_DUPLICATE_ERROR);
            }
            RouteAttraction relation = relationsById.get(item.getRelationId());
            if (relation == null) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_RELATION_NOT_EXIST);
            }

            changed |= !Objects.equals(relation.getDayNumber(), item.getDayNumber())
                    || !Objects.equals(relation.getVisitOrder(), item.getVisitOrder())
                    || item.getNotes() != null && !Objects.equals(relation.getNotes(), item.getNotes());
            relation.setDayNumber(item.getDayNumber());
            relation.setVisitOrder(item.getVisitOrder());
            if (item.getNotes() != null) {
                relation.setNotes(item.getNotes());
            }
        }

        validateCompleteSchedule(routeId, currentSchedule);
        if (!changed) {
            log.info("路线景点排序无需更新: routeId={}", routeId);
            return true;
        }

        replaceRouteSchedule(routeId, currentSchedule);
        log.info("路线景点排序完成: routeId={}, relations={}", routeId, currentSchedule.size());
        return true;
    }

    @Override
    public List<RouteAttraction> getByRouteIdOrderByDayAndVisit(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteAttraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteAttraction::getRouteId, routeId)
                .orderByAsc(RouteAttraction::getDayNumber, RouteAttraction::getVisitOrder); // 按天数+访问顺序排序

        return list(queryWrapper);
    }

    @Override
    public List<RouteAttraction> getByRouteIdOrderByDayAndVisitForUpdate(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteAttraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteAttraction::getRouteId, routeId)
                .orderByAsc(RouteAttraction::getDayNumber, RouteAttraction::getVisitOrder)
                .last("FOR UPDATE");
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceRouteSchedule(Integer routeId, List<RouteAttraction> routeAttractions) {
        validateCompleteSchedule(routeId, routeAttractions);

        List<RouteAttraction> currentSchedule = getByRouteIdOrderByDayAndVisitForUpdate(routeId.longValue());
        Set<Integer> currentRelationIds = currentSchedule.stream()
                .map(RouteAttraction::getId)
                .collect(Collectors.toSet());
        Set<Integer> requestedRelationIds = routeAttractions.stream()
                .map(RouteAttraction::getId)
                .collect(Collectors.toSet());
        if (!currentRelationIds.equals(requestedRelationIds)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_RELATION_NOT_EXIST);
        }

        int reservedRows = routeAttractionMapper.reserveVisitOrders(routeId);
        if (reservedRows != routeAttractions.size()) {
            log.error("路线日程预留顺序失败: routeId={}, expectedRows={}, actualRows={}",
                    routeId, routeAttractions.size(), reservedRows);
            throw new BusinessException(ErrorCodeEnum.ROUTE_UPDATE_FAILED);
        }
        if (!updateBatchById(routeAttractions)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceCompleteSchedule(Integer routeId, List<RouteAttraction> routeAttractions) {
        validateCompleteScheduleAllowEmpty(routeId, routeAttractions);

        routeAttractionMapper.delete(new LambdaQueryWrapper<RouteAttraction>()
                .eq(RouteAttraction::getRouteId, routeId));
        if (routeAttractions.isEmpty()) {
            return true;
        }

        for (RouteAttraction relation : routeAttractions) {
            relation.setId(null);
            relation.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (!saveBatch(routeAttractions)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_UPDATE_FAILED);
        }
        return true;
    }

    private void validateCompleteSchedule(Integer routeId, List<RouteAttraction> routeAttractions) {
        validateSchedule(routeId, routeAttractions, true);
        if (routeAttractions.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateCompleteScheduleAllowEmpty(Integer routeId, List<RouteAttraction> routeAttractions) {
        validateSchedule(routeId, routeAttractions, false);
    }

    private void validateSchedule(
            Integer routeId,
            List<RouteAttraction> routeAttractions,
            boolean requireExistingRelationId) {
        if (routeId == null || routeId <= 0 || routeAttractions == null || routeAttractions.size() > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Set<Integer> relationIds = new HashSet<>();
        Set<Integer> attractionIds = new HashSet<>();
        Map<Integer, Set<Integer>> ordersByDay = new HashMap<>();
        for (RouteAttraction relation : routeAttractions) {
            if (relation == null
                    || (requireExistingRelationId
                    && (relation.getId() == null || relation.getId() <= 0))
                    || !routeId.equals(relation.getRouteId())
                    || relation.getAttractionId() == null || relation.getAttractionId() <= 0
                    || relation.getDayNumber() == null || relation.getDayNumber() <= 0
                    || relation.getVisitOrder() == null || relation.getVisitOrder() <= 0) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            if ((requireExistingRelationId && !relationIds.add(relation.getId()))
                    || !attractionIds.add(relation.getAttractionId())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_DUPLICATE_ERROR);
            }
            Set<Integer> dayOrders = ordersByDay.computeIfAbsent(
                    relation.getDayNumber(), ignored -> new TreeSet<>());
            if (!dayOrders.add(relation.getVisitOrder())) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_ORDER_DUPLICATE);
            }
        }

        for (Set<Integer> dayOrders : ordersByDay.values()) {
            List<Integer> sortedOrders = new ArrayList<>(dayOrders);
            for (int index = 0; index < sortedOrders.size(); index++) {
                if (sortedOrders.get(index) != index + 1) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
                }
            }
        }
    }

    /**
     * 根据景点ID查询所有关联的路线-景点关系（使用routeAttractionMapper）
     * @param attractionId 景点ID
     * @return 路线-景点关系列表
     */
    @Override
    public List<RouteAttraction> getByAttractionId(Integer attractionId) {
        if (attractionId == null || attractionId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        // 使用routeAttractionMapper进行自定义查询
        LambdaQueryWrapper<RouteAttraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteAttraction::getAttractionId, attractionId);
        return routeAttractionMapper.selectList(queryWrapper);
    }

    /**
     * 统计每个景点出现在多少条路线中（使用routeAttractionMapper）
     * @param attractionIds 景点ID列表
     * @return 景点ID到出现次数的映射
     */
    @Override
    public Map<Integer, Integer> countRouteOccurrences(List<Integer> attractionIds) {
        if (attractionIds == null || attractionIds.isEmpty()) {
            return Map.of();
        }
        // 使用routeAttractionMapper进行自定义查询
        LambdaQueryWrapper<RouteAttraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(RouteAttraction::getAttractionId, attractionIds);
        List<RouteAttraction> routeAttractions = routeAttractionMapper.selectList(queryWrapper);
        // 统计每个景点的出现次数
        return routeAttractions.stream()
                .collect(Collectors.groupingBy(RouteAttraction::getAttractionId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }
}
