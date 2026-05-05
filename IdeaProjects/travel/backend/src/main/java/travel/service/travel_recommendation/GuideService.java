package travel.service.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_recommendation.Guide;

import java.util.List;
import java.util.Map;

public interface GuideService extends IService<Guide> {

    /**
     * 根据城市ID获取攻略列表
     */
    List<Guide> getByCityId(Integer cityId);

    /**
     * 根据攻略类型获取攻略列表
     */
    List<Guide> getByType(Integer cityId, String type);

    /**
     * 获取热门攻略列表
     */
    List<Guide> getHotGuides(int limit);

    /**
     * 获取最新攻略列表
     */
    List<Guide> getLatestGuides(int limit);

    /**
     * 搜索攻略
     */
    List<Guide> search(String keyword);

    /**
     * 获取攻略详情
     */
    Map<String, Object> getGuideDetail(Integer id);

    /**
     * 增加攻略浏览数
     */
    boolean incrementViews(Integer id);

    /**
     * 点赞攻略
     */
    boolean likeGuide(Integer id);

    /**
     * 推荐攻略
     */
    List<Map<String, Object>> recommendGuides(Integer cityId, Map<String, Object> preferences, int limit);
}
