package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import travel.entity.user_community.User;
import travel.service.user_community.TravelNoteService;
import travel.service.user_community.UserService;
import travel.utils.Result;

@Slf4j
@RestController
@RequestMapping("/travel-notes")
@RequiredArgsConstructor
public class TravelNoteCollectionController {

    private final TravelNoteService travelNoteService;
    private final UserService userService;

    @PostMapping("/{noteId}/collect")
    public Result<Boolean> collectNote(@PathVariable Integer noteId) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("请先登录");
            }

            log.info("收藏游记请求: noteId={}, userId={}", noteId, currentUser.getId());
            boolean result = travelNoteService.collectNote(noteId, currentUser.getId());
            return Result.success("收藏成功", result);
        } catch (Exception e) {
            log.error("收藏游记失败: noteId={}, error={}", noteId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/uncollect")
    public Result<Boolean> uncollectNote(@PathVariable Integer noteId) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("请先登录");
            }

            log.info("取消收藏游记请求: noteId={}, userId={}", noteId, currentUser.getId());
            boolean result = travelNoteService.uncollectNote(noteId, currentUser.getId());
            return Result.success("取消收藏成功", result);
        } catch (Exception e) {
            log.error("取消收藏游记失败: noteId={}, error={}", noteId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
