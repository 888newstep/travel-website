package travel.attraction.service;

import travel.common.entity.travel_recommendation.Attraction;

import java.util.List;
import java.util.Map;

/**
 * 景点POI详情服务
 * 提供开放时间、人流预测、游玩时长建议等详细信息
 */
public interface AttractionDetailService {

    /**
     * 获取景点详情
     * @param id 景点ID
     * @return 景点详情
     */
    Attraction getAttractionDetail(Long id);

    /**
     * 创建景点详情
     * @param detail 景点详情
     * @return 创建的景点
     */
    Attraction createAttractionDetail(Attraction detail);

    /**
     * 更新景点详情
     * @param id 景点ID
     * @param detail 景点详情
     * @return 更新的景点
     */
    Attraction updateAttractionDetail(Long id, Attraction detail);

    /**
     * 删除景点详情
     * @param id 景点ID
     * @return 是否删除成功
     */
    boolean deleteAttractionDetail(Long id);

    /**
     * 按城市查询景点
     * @param cityId 城市ID
     * @param page 页码
     * @param size 每页大小
     * @return 景点列表
     */
    List<Attraction> getAttractionsByCity(Integer cityId, int page, int size);

    /**
     * 搜索景点
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 景点列表
     */
    List<Attraction> searchAttractions(String keyword, int page, int size);

    /**
     * 获取景点图片
     * @param id 景点ID
     * @return 图片URL列表
     */
    List<String> getAttractionImages(Long id);

    /**
     * 获取景点评论
     * @param id 景点ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    List<Map<String, Object>> getAttractionReviews(Long id, int page, int size);

    /**
     * 获取景点评分统计
     * @param id 景点ID
     * @return 评分统计
     */
    Map<String, Object> getRatingStatistics(Long id);

    /**
     * 获取相似景点推荐
     * @param id 景点ID
     * @param limit 数量限制
     * @return 相似景点列表
     */
    List<Attraction> getSimilarAttractions(Long id, int limit);

    /**
     * 增加景点浏览量
     * @param id 景点ID
     * @return 是否成功
     */
    boolean incrementViews(Long id);

    /**
     * 批量更新景点
     * @param attractions 景点列表
     * @return 是否成功
     */
    boolean batchUpdateAttractions(List<Attraction> attractions);

    /**
     * 获取景点完整详情
     * @param attractionId 景点ID
     * @return 景点详情
     */
    Map<String, Object> getAttractionFullDetail(Integer attractionId);

    /**
     * 获取景点开放时间详情
     * @param attractionId 景点ID
     * @return 开放时间信息
     */
    Map<String, Object> getOpeningHoursDetail(Integer attractionId);

    /**
     * 获取人流预测
     * @param attractionId 景点ID
     * @param date 日期
     * @return 人流预测数据
     */
    Map<String, Object> getCrowdForecast(Integer attractionId, String date);

    /**
     * 获取建议游玩时长
     * @param attractionId 景点ID
     * @param visitorType 游客类型（family-家庭, couple-情侣, solo-独行, senior-老人）
     * @return 建议时长
     */
    Map<String, Object> getRecommendedVisitDuration(Integer attractionId, String visitorType);

    /**
     * 获取景点设施信息
     * @param attractionId 景点ID
     * @return 设施列表
     */
    List<Map<String, Object>> getAttractionFacilities(Integer attractionId);

    /**
     * 获取周边服务设施
     * @param attractionId 景点ID
     * @param serviceType 服务类型（restaurant-餐厅, parking-停车场, restroom-洗手间, shop-商店）
     * @return 周边服务列表
     */
    List<Map<String, Object>> getNearbyServices(Integer attractionId, String serviceType);

    /**
     * 获取景点最佳游览时间
     * @param attractionId 景点ID
     * @return 最佳时间建议
     */
    Map<String, Object> getBestVisitTime(Integer attractionId);

    /**
     * 获取景点拥挤度热力图数据
     * @param attractionId 景点ID
     * @return 热力图数据
     */
    Map<String, Object> getCrowdHeatmapData(Integer attractionId);

    /**
     * 获取景点季节性信息
     * @param attractionId 景点ID
     * @return 季节性建议
     */
    Map<String, Object> getSeasonalInfo(Integer attractionId);

    /**
     * 获取景点无障碍设施信息
     * @param attractionId 景点ID
     * @return 无障碍设施详情
     */
    Map<String, Object> getAccessibilityInfo(Integer attractionId);

    /**
     * 获取景点拍照打卡点
     * @param attractionId 景点ID
     * @return 打卡点列表
     */
    List<Map<String, Object>> getPhotoSpots(Integer attractionId);

    /**
     * 获取景点历史人流统计
     * @param attractionId 景点ID
     * @param days 统计天数
     * @return 历史数据
     */
    Map<String, Object> getHistoricalCrowdData(Integer attractionId, Integer days);
}
