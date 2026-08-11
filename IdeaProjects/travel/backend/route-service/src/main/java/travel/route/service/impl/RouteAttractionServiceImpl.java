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

import java.util.List;
import java.util.Map;
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
        // 1. 参数校验
        if (request == null || request.getRouteId() == null || request.getSortItems() == null || request.getSortItems().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 2. 验证路线存在
        if (routeService.getById(request.getRouteId()) == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        // 3. 验证同天数内顺序不重复
        Map<Integer, List<Integer>> dayOrderMap = request.getSortItems().stream()
                .collect(Collectors.groupingBy(
                        RouteAttractionBatchSortRequest.SortItem::getDayNumber,
                        Collectors.mapping(RouteAttractionBatchSortRequest.SortItem::getVisitOrder, Collectors.toList())
                ));
        for (List<Integer> orders : dayOrderMap.values()) {
            if (orders.size() != orders.stream().distinct().count()) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_ORDER_DUPLICATE);
            }
        }

        // 4. 构建更新列表
        List<RouteAttraction> updateList = request.getSortItems().stream().map(item -> {
            RouteAttraction relation = getById(item.getRelationId());
            if (relation == null) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_RELATION_NOT_EXIST);
            }
            // 更新天数、顺序、备注
            relation.setDayNumber(item.getDayNumber());
            relation.setVisitOrder(item.getVisitOrder());
            if (item.getNotes() != null) {
                relation.setNotes(item.getNotes());
            }
            return relation;
        }).collect(Collectors.toList());

        // 5. 批量更新
        boolean success = updateBatchById(updateList);
        log.info("路线{}的景点排序完成，共更新{}个关联关系", request.getRouteId(), updateList.size());
        return success;
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
