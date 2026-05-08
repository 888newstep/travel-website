package travel.controller.user_community_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import travel.entity.user_community.TravelNote;
import travel.service.user_community.TravelNoteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/travel-notes/{noteId}/tags")
@Tag(name = "游记标签管理", description = "游记标签相关接口")
public class TravelNoteTagController {

    @Autowired
    private TravelNoteService travelNoteService;

    @PostMapping
    @Operation(summary = "为游记添加标签")
    public ResponseEntity<Map<String, Object>> addTags(
            @Parameter(description = "游记ID") @PathVariable Integer noteId,
            @Parameter(description = "标签列表") @RequestBody List<String> tags) {
        TravelNote travelNote = new TravelNote();
        travelNote.setId(noteId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "标签添加功能需要在更新游记时实现");
        result.put("data", tags);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "获取游记的标签列表")
    public ResponseEntity<Map<String, Object>> getTags(
            @Parameter(description = "游记ID") @PathVariable Integer noteId) {
        Map<String, Object> detail = travelNoteService.getTravelNoteDetail(noteId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", detail.get("tags"));
        return ResponseEntity.ok(result);
    }
}
