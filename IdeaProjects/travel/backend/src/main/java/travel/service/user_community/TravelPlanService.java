package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.TravelPlan;

import java.util.List;

public interface TravelPlanService extends IService<TravelPlan> {

    /**
     * 创建行程
     */
    TravelPlan createPlan(TravelPlan plan);

    /**
     * 获取用户的所有行程
     */
    List<TravelPlan> getByUserId(Integer userId);

    /**
     * 获取当前用户的所有行程
     */
    List<TravelPlan> getCurrentUserPlans();

    /**
     * 更新行程
     */
    TravelPlan updatePlan(TravelPlan plan);

    /**
     * 删除行程
     */
    boolean deletePlan(Integer id);

    /**
     * 获取行程详情
     */
    TravelPlan getPlanDetail(Integer id);

    /**
     * 分享行程
     */
    String sharePlan(Integer id);

    /**
     * 复制行程
     */
    TravelPlan copyPlan(Integer id);
}