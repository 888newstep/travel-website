package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.route_planning.Route;
import travel.exception.BusinessException;
import travel.utils.Result;

import java.util.List;
import java.util.Map;

/**
 * 路线核心服务接口
 */
public interface RouteService extends IService<Route> {

    /**
     * 根据ID查询路线
     * @param id 路线ID
     * @return 路线信息
     */
    Route getById(Integer id);

    /**
     * 根据用户ID查询我的路线
     * @param userId 用户ID
     * @return 路线列表
     */
    List<Route> getMyRoutes(Long userId);

    /**
     * 校验路线归属（确保用户只能操作自己的路线）
     * @param routeId 路线ID
     * @param userId 用户ID
     * @throws BusinessException 无权限时抛出异常
     */
    void checkRouteOwner(Long routeId, Long userId);

    /**
     * 根据路线标题模糊查询路线
     * @param title 路线标题
     * @return 路线列表
     */
    List<Route> searchRoutesByTitle(String title);

    /**
     * 获取用户创建的路线数量
     * @param userId 用户ID
     * @return 路线数量
     */
    int getUserRouteCount(Long userId);

    /**
     * 根据城市ID查询路线
     * @param cityId 城市ID
     * @return 路线列表
     */
    List<Route> getByCityId(Integer cityId);

    /**
     * 保存路线
     * @param route 路线信息
     * @return 是否保存成功
     */
    boolean save(Route route);

    /**
     * 更新路线
     * @param route 路线信息
     * @return 是否更新成功
     */
    boolean updateById(Route route);

    /**
     * 根据ID删除路线
     * @param id 路线ID
     * @return 是否删除成功
     */
    boolean removeById(Integer id);

    /**
     * 根据ID列表批量查询路线
     * @param routeIds 路线ID列表
     * @return 路线列表
     */
    List<Route> listByIds(List<Integer> routeIds);

    /**
     * 获取路线统计信息
     * @return 统计信息
     */
    Map<String, Object> getRouteStatistics();

    /**
     * 根据城市获取路线统计
     * @return 城市路线统计
     */
    List<Map<String, Object>> getRouteStatisticsByCity();

    /**
     * 获取路线完成率
     * @return 完成率信息
     */
    Map<String, Object> getRouteCompletionRate();

    /**
     * 获取路线时长分布
     * @return 时长分布信息
     */
    List<Map<String, Object>> getRouteDurationDistribution();

    /**
     * 获取交通方式选项
     * @param fromCity 出发城市ID
     * @param toCity 目的城市ID
     * @return 交通方式列表
     */
    List<Map<String, Object>> getTransportOptions(Integer fromCity, Integer toCity);

    /**
     * 计算交通费用
     * @param params 计算参数
     * @return 费用信息
     */
    Map<String, Object> calculateTransportCost(Map<String, Object> params);

    /**
     * 计算交通时间
     * @param params 计算参数
     * @return 时间信息
     */
    Map<String, Object> calculateTransportTime(Map<String, Object> params);

    /**
     * 获取交通推荐
     * @param params 推荐参数
     * @return 交通推荐列表
     */
    List<Map<String, Object>> getTransportRecommendations(Map<String, Object> params);

    /**
     * 创建旅行协作
     * @param params 协作参数
     * @return 协作信息
     */
    Map<String, Object> createTripCollaboration(Map<String, Object> params);

    /**
     * 获取旅行协作
     * @param collaborationId 协作ID
     * @return 协作信息
     */
    Map<String, Object> getTripCollaboration(Long collaborationId);

    /**
     * 获取用户旅行协作列表
     * @param userId 用户ID
     * @return 协作列表
     */
    List<Map<String, Object>> getUserTripCollaborations(Long userId);

    /**
     * 邀请协作者
     * @param collaborationId 协作ID
     * @param collaboratorId 协作者ID
     * @return 邀请结果
     */
    Map<String, Object> inviteCollaborator(Long collaborationId, Long collaboratorId);

    /**
     * 更新旅行协作
     * @param collaborationId 协作ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateTripCollaboration(Long collaborationId, Map<String, Object> params);

    /**
     * 删除旅行协作
     * @param collaborationId 协作ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Map<String, Object> deleteTripCollaboration(Long collaborationId, Long userId);

    /**
     * 获取需要同步的路线
     * @param minutes 分钟数
     * @return 需要同步的路线列表
     */
    List<Map<String, Object>> getRoutesNeedingSync(Integer minutes);

    /**
     * 同步路线状态
     * @param routeIds 路线ID列表
     * @return 同步结果
     */
    Map<String, Object> syncRouteStatus(List<Integer> routeIds);

    /**
     * 获取路线实时状态
     * @param routeId 路线ID
     * @return 实时状态信息
     */
    Map<String, Object> getRouteRealtimeStatus(Integer routeId);

    /**
     * 更新路线实时状态
     * @param routeId 路线ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateRouteRealtimeStatus(Integer routeId, Map<String, Object> params);

    /**
     * 生成多模态路线
     * @param params 生成参数
     * @return 多模态路线信息
     */
    Map<String, Object> generateMultimodalRoute(Map<String, Object> params);

    /**
     * 分析多模态输入
     * @param params 分析参数
     * @return 分析结果
     */
    Map<String, Object> analyzeMultimodalInput(Map<String, Object> params);

    /**
     * 分析旅行图片
     * @param imageUrl 图片URL
     * @return 分析结果
     */
    Map<String, Object> analyzeTravelImage(String imageUrl);

    /**
     * 基于图片生成路线
     * @param imageUrl 图片URL
     * @param preferences 用户偏好
     * @return 生成的路线
     */
    Map<String, Object> generateRouteFromImage(String imageUrl, Map<String, Object> preferences);

    /**
     * 获取景点详情
     * @param attractionId 景点ID
     * @return 景点详情
     */
    Map<String, Object> getAttractionDetail(Integer attractionId);

    /**
     * 获取景点评论
     * @param attractionId 景点ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    Map<String, Object> getAttractionReviews(Integer attractionId, int page, int size);

    /**
     * 获取附近景点
     * @param attractionId 景点ID
     * @param radius 半径（米）
     * @return 附近景点列表
     */
    Map<String, Object> getNearbyAttractions(Integer attractionId, int radius);

    /**
     * 添加景点评论
     * @param params 评论参数
     * @return 添加结果
     */
    Map<String, Object> addAttractionReview(Map<String, Object> params);

    /**
     * 预测路线热度
     * @param routeId 路线ID
     * @return 预测结果
     */
    Map<String, Object> predictRoutePopularity(Integer routeId);

    /**
     * 生成路线变体
     * @param routeId 路线ID
     * @param count 变体数量
     * @return 路线变体列表
     */
    List<Map<String, Object>> generateRouteVariations(Integer routeId, int count);

    /**
     * 获取文件分类
     * @return 文件分类列表
     */
    List<Map<String, Object>> getFileCategories();

    /**
     * 创建文件分类
     * @param params 分类参数
     * @return 分类信息
     */
    Map<String, Object> createFileCategory(Map<String, Object> params);

    /**
     * 更新文件分类
     * @param categoryId 分类ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateFileCategory(Long categoryId, Map<String, Object> params);

    /**
     * 删除文件分类
     * @param categoryId 分类ID
     * @return 删除结果
     */
    Map<String, Object> deleteFileCategory(Long categoryId);

    /**
     * 获取文件版本
     * @param fileId 文件ID
     * @return 文件版本列表
     */
    List<Map<String, Object>> getFileVersions(Long fileId);

    /**
     * 创建文件版本
     * @param params 版本参数
     * @return 版本信息
     */
    Map<String, Object> createFileVersion(Map<String, Object> params);

    /**
     * 获取文件版本
     * @param versionId 版本ID
     * @return 版本信息
     */
    Map<String, Object> getFileVersion(Long versionId);

    /**
     * 删除文件版本
     * @param versionId 版本ID
     * @return 删除结果
     */
    Map<String, Object> deleteFileVersion(Long versionId);

    /**
     * 上传文件
     * @param params 文件参数
     * @return 文件信息
     */
    Map<String, Object> uploadFile(Map<String, Object> params);

    /**
     * 获取文件
     * @param fileId 文件ID
     * @return 文件信息
     */
    Map<String, Object> getFile(Long fileId);

    /**
     * 获取文件列表
     * @param params 查询参数
     * @return 文件列表
     */
    List<Map<String, Object>> getFiles(Map<String, Object> params);

    /**
     * 更新文件
     * @param fileId 文件ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateFile(Long fileId, Map<String, Object> params);

    /**
     * 删除文件
     * @param fileId 文件ID
     * @return 删除结果
     */
    Map<String, Object> deleteFile(Long fileId);

    /**
     * 提交反馈
     * @param params 反馈参数
     * @return 反馈信息
     */
    Result<Map<String, Object>> submitFeedback(Map<String, Object> params);

    /**
     * 获取反馈列表
     * @param page 页码
     * @param size 每页大小
     * @return 反馈列表
     */
    Result<List<Map<String, Object>>> getFeedbacks(int page, int size);

    /**
     * 获取用户反馈列表
     * @param userId 用户ID
     * @return 反馈列表
     */
    Result<List<Map<String, Object>>> getUserFeedbacks(Long userId);

    /**
     * 分享文件
     * @param params 分享参数
     * @return 分享信息
     */
    Map<String, Object> shareFile(Map<String, Object> params);

    /**
     * 获取共享文件列表
     * @param userId 用户ID
     * @return 共享文件列表
     */
    List<Map<String, Object>> getSharedFiles(Long userId);

    /**
     * 撤销文件分享
     * @param shareId 分享ID
     * @return 撤销结果
     */
    Map<String, Object> revokeFileShare(Long shareId);

    /**
     * 收藏路线
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 收藏信息
     */
    Map<String, Object> collectRoute(Long routeId, Long userId);

    /**
     * 获取用户路线收藏列表
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<Map<String, Object>> getUserRouteCollections(Long userId);

    /**
     * 取消路线收藏
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 取消结果
     */
    Map<String, Object> cancelRouteCollection(Long routeId, Long userId);

    /**
     * 添加路线评论
     * @param params 评论参数
     * @return 评论信息
     */
    Map<String, Object> addRouteComment(Map<String, Object> params);

    /**
     * 获取路线评论列表
     * @param routeId 路线ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    List<Map<String, Object>> getRouteComments(Long routeId, int page, int size);

    /**
     * 更新路线评论
     * @param commentId 评论ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateRouteComment(Long commentId, Map<String, Object> params);

    /**
     * 删除路线评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Map<String, Object> deleteRouteComment(Long commentId, Long userId);

    /**
     * 分享路线
     * @param params 分享参数
     * @return 分享信息
     */
    Map<String, Object> shareRoute(Map<String, Object> params);

    /**
     * 获取共享路线
     * @param shareUrl 分享URL
     * @return 路线信息
     */
    Map<String, Object> getSharedRoute(String shareUrl);

    /**
     * 获取用户共享路线列表
     * @param userId 用户ID
     * @return 共享路线列表
     */
    List<Map<String, Object>> getUserSharedRoutes(Long userId);

    /**
     * 取消路线分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 取消结果
     */
    Map<String, Object> unshareRoute(Long shareId, Long userId);

    /**
     * 创建旅行笔记
     * @param params 笔记参数
     * @return 笔记信息
     */
    Map<String, Object> createTravelNote(Map<String, Object> params);

    /**
     * 获取旅行笔记
     * @param noteId 笔记ID
     * @return 笔记信息
     */
    Map<String, Object> getTravelNote(Long noteId);

    /**
     * 获取用户旅行笔记列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    List<Map<String, Object>> getUserTravelNotes(Long userId, int page, int size);

    /**
     * 更新旅行笔记
     * @param noteId 笔记ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateTravelNote(Long noteId, Map<String, Object> params);

    /**
     * 删除旅行笔记
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Map<String, Object> deleteTravelNote(Long noteId, Long userId);
}
