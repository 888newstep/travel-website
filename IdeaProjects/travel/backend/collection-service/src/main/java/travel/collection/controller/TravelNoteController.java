package travel.collection.controller;

import travel.common.exception.ExceptionPropagation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.collection.dto.TravelNoteWriteRequest;
import travel.common.entity.user_community.TravelNote;
import travel.common.security.AuthenticatedUserSupport;
import travel.collection.service.TravelNoteService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "旅游笔记管理", description = "游记CRUD、收藏、点赞、标签等接口")
public class TravelNoteController {

    private final TravelNoteService travelNoteService;

    /**
     * 创建游记
     * POST /api/travel-notes
     */
    @PostMapping
    public Result<TravelNote> createTravelNote(@Valid @RequestBody TravelNoteWriteRequest request) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("创建游记请求: userId={}", userId);
        TravelNote created = travelNoteService.createTravelNote(
                userId, request.getTravelNote().toEntity(), request.getTags());
        return Result.success("创建游记成功", created);
    }

    /**
     * 更新游记
     * PUT /api/travel-notes/{id}
     */
    @PutMapping("/{id}")
    public Result<TravelNote> updateTravelNote(@PathVariable Integer id,
                                               @Valid @RequestBody TravelNoteWriteRequest request) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("更新游记请求: id={}, userId={}", id, userId);
        TravelNote updated = travelNoteService.updateTravelNote(
                id, userId, request.getTravelNote().toEntity(), request.getTags());
        return Result.success("更新游记成功", updated);
    }

    /**
     * 删除游记
     * DELETE /api/travel-notes/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTravelNote(@PathVariable Integer id) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("删除游记请求: id={}, userId={}", id, userId);
        boolean result = travelNoteService.deleteTravelNote(id, userId);
        return Result.success("删除游记成功", result);
    }

    /**
     * 获取游记详情
     * GET /api/travel-notes/{id}
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTravelNoteDetail(@PathVariable Integer id) {
        try {
            log.info("获取游记详情请求: id={}", id);
            Integer currentUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
            Map<String, Object> detail = travelNoteService.getTravelNoteDetail(id, currentUserId);
            return Result.success("获取游记详情成功", detail);
        } catch (Exception e) {
            log.error("获取游记详情失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
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
            Integer currentUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
            List<Map<String, Object>> notes = travelNoteService.getUserTravelNotes(userId, currentUserId, page, size);
            return Result.success("获取用户游记列表成功", notes);
        } catch (Exception e) {
            log.error("获取用户游记列表失败: userId={}, error={}", userId, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 切换游记点赞状态（合并 like/unlike）
     * POST /api/travel-notes/{id}/toggle-like
     */
    @PostMapping("/{id}/toggle-like")
    public Result<Map<String, Object>> toggleLikeTravelNote(@PathVariable Integer id) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("切换游记点赞状态: id={}, userId={}", id, userId);
            Map<String, Object> result = travelNoteService.toggleLikeTravelNote(id, userId);
            return Result.success(result.get("liked").equals(true) ? "点赞成功" : "取消点赞成功", result);
        } catch (Exception e) {
            log.error("切换游记点赞状态失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
        }
    }

    // ==================== 收藏功能 ====================

    @PostMapping("/{noteId}/toggle-collect")
    @Operation(summary = "切换游记收藏状态（合并 collect/uncollect）")
    public Result<Map<String, Object>> toggleCollectNote(@PathVariable Integer noteId) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("切换游记收藏状态: noteId={}, userId={}", noteId, userId);
            Map<String, Object> result = travelNoteService.toggleCollectNote(noteId, userId);
            return Result.success(result.get("collected").equals(true) ? "收藏成功" : "取消收藏成功", result);
        } catch (Exception e) {
            log.error("切换游记收藏状态失败: noteId={}, error={}", noteId, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }
}
