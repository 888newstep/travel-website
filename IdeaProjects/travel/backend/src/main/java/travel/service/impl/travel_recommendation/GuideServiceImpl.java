package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import travel.entity.travel_recommendation.Guide;
import travel.mapper.travel_recommendation_mapper.GuideMapper;
import travel.service.travel_recommendation.GuideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GuideServiceImpl extends ServiceImpl<GuideMapper, Guide> implements GuideService {

    private static final Logger log = LoggerFactory.getLogger(GuideServiceImpl.class);

    @Override
    public List<Guide> getByCityId(Integer cityId) {
        try {
            QueryWrapper<Guide> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.orderByDesc("created_at");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取城市攻略列表失败: cityId={}", cityId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Guide> getByType(Integer cityId, String type) {
        try {
            QueryWrapper<Guide> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("city_id", cityId);
            queryWrapper.eq("type", type);
            queryWrapper.orderByDesc("created_at");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取攻略类型列表失败: cityId={}, type={}", cityId, type, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Guide> getHotGuides(int limit) {
        try {
            QueryWrapper<Guide> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("views_count", "likes_count", "comments_count");
            queryWrapper.last("LIMIT " + limit);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取热门攻略列表失败: limit={}", limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Guide> getLatestGuides(int limit) {
        try {
            QueryWrapper<Guide> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("created_at");
            queryWrapper.last("LIMIT " + limit);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("获取最新攻略列表失败: limit={}", limit, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Guide> search(String keyword) {
        try {
            QueryWrapper<Guide> queryWrapper = new QueryWrapper<>();
            queryWrapper.like("title", keyword).or().like("content", keyword);
            queryWrapper.orderByDesc("created_at");
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("搜索攻略失败: keyword={}", keyword, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getGuideDetail(Integer id) {
        try {
            Guide guide = getById(id);
            if (guide == null) {
                throw new RuntimeException("攻略不存在");
            }

            incrementViews(id);

            Map<String, Object> result = new HashMap<>();
            result.put("guide", guide);

            return result;
        } catch (Exception e) {
            log.error("获取攻略详情失败: id={}", id, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean incrementViews(Integer id) {
        try {
            Guide guide = getById(id);
            if (guide == null) {
                return false;
            }

            guide.setViewsCount(guide.getViewsCount() + 1);
            return updateById(guide);
        } catch (Exception e) {
            log.error("增加攻略浏览数失败: id={}", id, e);
            return false;
        }
    }

    @Override
    public boolean likeGuide(Integer id) {
        try {
            Guide guide = getById(id);
            if (guide == null) {
                return false;
            }

            guide.setLikesCount(guide.getLikesCount() + 1);
            return updateById(guide);
        } catch (Exception e) {
            log.error("点赞攻略失败: id={}", id, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> recommendGuides(Integer cityId, Map<String, Object> preferences, int limit) {
        try {
            List<Guide> guides = getByCityId(cityId);

            List<Map<String, Object>> result = guides.stream()
                    .map(guide -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("guide", guide);
                        double score = calculateRecommendationScore(guide, preferences);
                        map.put("score", score);
                        return map;
                    })
                    .sorted((m1, m2) -> Double.compare((Double) m2.get("score"), (Double) m1.get("score")))
                    .limit(limit)
                    .collect(Collectors.toList());

            return result;
        } catch (Exception e) {
            log.error("推荐攻略失败: cityId={}, preferences={}, limit={}", cityId, preferences, limit, e);
            return new ArrayList<>();
        }
    }

    private double calculateRecommendationScore(Guide guide, Map<String, Object> preferences) {
        double score = 0.0;

        score += guide.getViewsCount() * 0.1 + guide.getLikesCount() * 0.5;

        if (preferences != null) {
            if (preferences.containsKey("type")) {
                String preferredType = (String) preferences.get("type");
                if (preferredType.equals(guide.getType())) {
                    score += 10;
                }
            }
        }

        return score;
    }
}