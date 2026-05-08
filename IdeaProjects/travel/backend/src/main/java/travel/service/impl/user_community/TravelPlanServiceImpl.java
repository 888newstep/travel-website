package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.TravelPlan;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.user_community_mapper.TravelPlanMapper;
import travel.service.user_community.TravelPlanService;
import travel.service.user_community.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanServiceImpl extends ServiceImpl<TravelPlanMapper, TravelPlan> implements TravelPlanService {

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TravelPlan createPlan(TravelPlan plan) {
        User currentUser = userService.getCurrentUser();
        plan.setUserId(currentUser.getId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setStatus("planning");

        save(plan);
        return plan;
    }

    @Override
    public List<TravelPlan> getByUserId(Integer userId) {
        LambdaQueryWrapper<TravelPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TravelPlan::getUserId, userId);
        queryWrapper.orderByDesc(TravelPlan::getCreatedAt);
        return list(queryWrapper);
    }

    @Override
    public List<TravelPlan> getCurrentUserPlans() {
        User currentUser = userService.getCurrentUser();
        return getByUserId(currentUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TravelPlan updatePlan(TravelPlan plan) {
        TravelPlan existingPlan = getById(plan.getId());
        if (existingPlan == null) {
            throw new BusinessException(ErrorCodeEnum.PLAN_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!existingPlan.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        plan.setUpdatedAt(LocalDateTime.now());
        updateById(plan);
        return getById(plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlan(Integer id) {
        TravelPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCodeEnum.PLAN_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!plan.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return removeById(id);
    }

    @Override
    public TravelPlan getPlanDetail(Integer id) {
        TravelPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCodeEnum.PLAN_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!plan.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return plan;
    }

    @Override
    public String sharePlan(Integer id) {
        TravelPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCodeEnum.PLAN_NOT_EXIST);
        }

        // 生成分享链接
        // 实际项目中应该使用更安全的方式生成分享token
        return "https://travel.example.com/share/" + id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TravelPlan copyPlan(Integer id) {
        TravelPlan originalPlan = getById(id);
        if (originalPlan == null) {
            throw new BusinessException(ErrorCodeEnum.PLAN_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();

        // 创建新行程
        TravelPlan newPlan = new TravelPlan();
        newPlan.setTitle(originalPlan.getTitle() + " (复制)");
        newPlan.setDescription(originalPlan.getDescription());
        newPlan.setUserId(currentUser.getId());
        newPlan.setStartDate(originalPlan.getStartDate());
        newPlan.setEndDate(originalPlan.getEndDate());
        newPlan.setDestinations(originalPlan.getDestinations());
        newPlan.setStatus("planning");
        newPlan.setCoverImage(originalPlan.getCoverImage());
        newPlan.setCreatedAt(LocalDateTime.now());
        newPlan.setUpdatedAt(LocalDateTime.now());

        save(newPlan);
        return newPlan;
    }
}