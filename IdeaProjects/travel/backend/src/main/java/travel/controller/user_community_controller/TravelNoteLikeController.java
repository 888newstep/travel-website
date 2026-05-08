package travel.controller.user_community_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import travel.service.user_community.TravelNoteService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/travel-notes/{noteId}/likes")
@Tag(name = "游记点赞管理", description = "游记点赞相关接口")
public class TravelNoteLikeController {

    @Autowired
    private TravelNoteService travelNoteService;

    @PostMapping
    @Operation(summary = "点赞游记")
    public ResponseEntity<Map<String, Object>> likeTravelNote(
            @Parameter(description = "游记ID") @PathVariable Integer noteId) {
        boolean success = travelNoteService.likeTravelNote(noteId, null);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "点赞成功");
        } else {
            result.put("code", 500);
            result.put("message", "点赞失败");
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    @Operation(summary = "取消点赞游记")
    public ResponseEntity<Map<String, Object>> unlikeTravelNote(
            @Parameter(description = "游记ID") @PathVariable Integer noteId) {
        boolean success = travelNoteService.unlikeTravelNote(noteId, null);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "取消点赞成功");
        } else {
            result.put("code", 500);
            result.put("message", "取消点赞失败");
        }
        return ResponseEntity.ok(result);
    }
}
