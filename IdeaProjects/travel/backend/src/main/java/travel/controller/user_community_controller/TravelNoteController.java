package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.TravelNote;
import travel.service.user_community.TravelNoteService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 旅游笔记控制器
 * 管理用户的旅游游记和笔记
 */
@RestController
@RequestMapping("/travel-notes")
@Slf4j
@RequiredArgsConstructor
public class TravelNoteController {

    private final TravelNoteService travelNoteService;

    /**
     * 创建游记
     * POST /api/travel-notes
     */
    @PostMapping
    public Result<TravelNote> createTravelNote(@RequestBody Map<String, Object> request) {
        try {
            TravelNote travelNote = (TravelNote) request.get("travelNote");
            List<String> tags = null;
            Object tagsObj = request.get("tags");
            if (tagsObj instanceof List) {
                tags = new ArrayList<>();
                for (Object item : (List<?>) tagsObj) {
                    if (item instanceof String) {
                        tags.add((String) item);
                    }
                }
            }
            log.info("创建游记请求: userId={}", travelNote.getUserId());
            TravelNote created = travelNoteService.createTravelNote(travelNote, tags);
            return Result.success("创建游记成功", created);
        } catch (Exception e) {
            log.error("创建游记失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新游记
     * PUT /api/travel-notes/{id}
     */
    @PutMapping("/{id}")
    public Result<TravelNote> updateTravelNote(@PathVariable Integer id,
                                               @RequestBody Map<String, Object> request) {
        try {
            TravelNote travelNote = (TravelNote) request.get("travelNote");
            List<String> tags = null;
            Object tagsObj = request.get("tags");
            if (tagsObj instanceof List) {
                tags = new ArrayList<>();
                for (Object item : (List<?>) tagsObj) {
                    if (item instanceof String) {
                        tags.add((String) item);
                    }
                }
            }
            log.info("更新游记请求: id={}", id);
            TravelNote updated = travelNoteService.updateTravelNote(id, travelNote, tags);
            return Result.success("更新游记成功", updated);
        } catch (Exception e) {
            log.error("更新游记失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除游记
     * DELETE /api/travel-notes/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTravelNote(@PathVariable Integer id,
                                            @RequestParam Integer userId) {
        try {
            log.info("删除游记请求: id={}, userId={}", id, userId);
            boolean result = travelNoteService.deleteTravelNote(id, userId);
            return Result.success("删除游记成功", result);
        } catch (Exception e) {
            log.error("删除游记失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取游记详情
     * GET /api/travel-notes/{id}
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTravelNoteDetail(@PathVariable Integer id) {
        try {
            log.info("获取游记详情请求: id={}", id);
            Map<String, Object> detail = travelNoteService.getTravelNoteDetail(id);
            return Result.success("获取游记详情成功", detail);
        } catch (Exception e) {
            log.error("获取游记详情失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页获取游记列表
     * GET /api/travel-notes/list
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getTravelNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Map<String, Object> filters) {
        try {
            log.info("分页获取游记列表请求: page={}, size={}", page, size);
            List<Map<String, Object>> notes = travelNoteService.getTravelNotes(page, size, filters);
            return Result.success("获取游记列表成功", notes);
        } catch (Exception e) {
            log.error("获取游记列表失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的游记列表
     * GET /api/travel-notes/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<Map<String, Object>>> getUserTravelNotes(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("获取用户游记列表请求: userId={}", userId);
            List<Map<String, Object>> notes = travelNoteService.getUserTravelNotes(userId, page, size);
            return Result.success("获取用户游记列表成功", notes);
        } catch (Exception e) {
            log.error("获取用户游记列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 点赞游记
     * POST /api/travel-notes/{id}/like
     */
    @PostMapping("/{id}/like")
    public Result<Boolean> likeTravelNote(@PathVariable Integer id,
                                          @RequestParam Integer userId) {
        try {
            log.info("点赞游记请求: id={}, userId={}", id, userId);
            boolean result = travelNoteService.likeTravelNote(id, userId);
            return Result.success("点赞游记成功", result);
        } catch (Exception e) {
            log.error("点赞游记失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消点赞游记
     * POST /api/travel-notes/{id}/unlike
     */
    @PostMapping("/{id}/unlike")
    public Result<Boolean> unlikeTravelNote(@PathVariable Integer id,
                                             @RequestParam Integer userId) {
        try {
            log.info("取消点赞游记请求: id={}, userId={}", id, userId);
            boolean result = travelNoteService.unlikeTravelNote(id, userId);
            return Result.success("取消点赞游记成功", result);
        } catch (Exception e) {
            log.error("取消点赞游记失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 增加游记浏览数
     * POST /api/travel-notes/{id}/view
     */
    @PostMapping("/{id}/view")
    public Result<Boolean> incrementViews(@PathVariable Integer id) {
        try {
            log.info("增加游记浏览数请求: id={}", id);
            boolean result = travelNoteService.incrementViews(id);
            return Result.success("增加游记浏览数成功", result);
        } catch (Exception e) {
            log.error("增加游记浏览数失败: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索游记
     * GET /api/travel-notes/search
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchTravelNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("搜索游记请求: keyword={}, page={}, size={}", keyword, page, size);
            List<Map<String, Object>> notes = travelNoteService.searchTravelNotes(keyword, page, size);
            return Result.success("搜索游记成功", notes);
        } catch (Exception e) {
            log.error("搜索游记失败: keyword={}, error={}", keyword, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取热门游记
     * GET /api/travel-notes/hot
     */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotTravelNotes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("获取热门游记请求: limit={}", limit);
            List<Map<String, Object>> notes = travelNoteService.getHotTravelNotes(limit);
            return Result.success("获取热门游记成功", notes);
        } catch (Exception e) {
            log.error("获取热门游记失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取最新游记
     * GET /api/travel-notes/latest
     */
    @GetMapping("/latest")
    public Result<List<Map<String, Object>>> getLatestTravelNotes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("获取最新游记请求: limit={}", limit);
            List<Map<String, Object>> notes = travelNoteService.getLatestTravelNotes(limit);
            return Result.success("获取最新游记成功", notes);
        } catch (Exception e) {
            log.error("获取最新游记失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
