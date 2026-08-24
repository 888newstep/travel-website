package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.TravelNote;

import java.util.List;
import java.util.Map;

public interface TravelNoteService extends IService<TravelNote> {

    /**
     * 创建游记
     */
    TravelNote createTravelNote(Integer userId, TravelNote travelNote, List<String> tags);

    /**
     * 收藏游记
     */
    boolean collectNote(Integer noteId, Integer userId);

    /**
     * 取消收藏游记
     */
    boolean uncollectNote(Integer noteId, Integer userId);

    /**
     * 切换游记收藏状态（合并 collect/uncollect）
     */
    Map<String, Object> toggleCollectNote(Integer noteId, Integer userId);

    /**
     * 更新游记
     */
    TravelNote updateTravelNote(Integer id, Integer userId, TravelNote travelNote, List<String> tags);

    /**
     * 删除游记
     */
    boolean deleteTravelNote(Integer id, Integer userId);

    /**
     * 获取游记详情
     */
    Map<String, Object> getTravelNoteDetail(Integer id, Integer currentUserId);

    /**
     * 分页获取游记列表
     */
    List<Map<String, Object>> getTravelNotes(int page, int size, Map<String, Object> filters);

    /**
     * 获取用户的游记列表
     */
    List<Map<String, Object>> getUserTravelNotes(Integer userId, Integer currentUserId, int page, int size);

    /**
     * 点赞游记
     */
    boolean likeTravelNote(Integer id, Integer userId);

    /**
     * 取消点赞游记
     */
    boolean unlikeTravelNote(Integer id, Integer userId);

    /**
     * 切换游记点赞状态（合并 like/unlike）
     */
    Map<String, Object> toggleLikeTravelNote(Integer id, Integer userId);

    /**
     * 增加游记浏览数
     */
    boolean incrementViews(Integer id);

    /**
     * 搜索游记
     */
    List<Map<String, Object>> searchTravelNotes(String keyword, int page, int size);

    /**
     * 获取热门游记
     */
    List<Map<String, Object>> getHotTravelNotes(int limit);

    /**
     * 获取最新游记
     */
    List<Map<String, Object>> getLatestTravelNotes(int limit);

    /**
     * 统计用户的游记数量
     */
    int countByUserId(Integer userId);
}
