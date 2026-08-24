package travel.attraction.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import lombok.RequiredArgsConstructor;
import travel.common.config.SentinelFallbackHandler;
import travel.common.entity.travel_recommendation.Attraction;
import travel.attraction.service.AttractionDetailService;
import travel.attraction.service.AttractionService;
import travel.common.utils.Result;
import travel.common.security.AuthenticatedUserSupport;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import travel.common.vo.CursorPageResult;


@RestController
@RequestMapping("/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;
    private final AttractionDetailService attractionDetailService;

    // ==================== Basic CRUD ====================

    @GetMapping
    public Result<List<Attraction>> getAttractions() {
        List<Attraction> attractions = attractionService.list();
        return Result.success("Fetched attractions successfully", attractions);
    }

    @SentinelResource(value = "getAttractionById",
            fallback = "getAttractionByIdFallback", blockHandler = "getAttractionByIdFallback")
    @GetMapping("/{id}")
    public Result<Attraction> getAttraction(@PathVariable Integer id) {
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            return Result.success("Fetched attraction successfully", attraction);
        }
        return Result.error("Attraction not found");
    }

    public Result<Attraction> getAttractionByIdFallback(Integer id, Throwable e) {
        return SentinelFallbackHandler.getAttractionByIdFallback(id, null);
    }

    @SentinelResource(value = "getAttractionsByCity",
            fallback = "getAttractionsByCityFallback", blockHandler = "getAttractionsByCityFallback")
    @GetMapping("/city/{cityId}")
    public Result<List<Attraction>> getAttractionsByCity(@PathVariable Integer cityId) {
        List<Attraction> attractions = attractionService.getByCityId(cityId);
        return Result.success("Fetched city attractions successfully", attractions);
    }

    public Result<List<Attraction>> getAttractionsByCityFallback(Integer cityId, Throwable e) {
        return SentinelFallbackHandler.getAttractionsByCityFallback(cityId, null);
    }

    @SentinelResource(value = "searchAttractions",
            fallback = "searchAttractionsFallback", blockHandler = "searchAttractionsFallback")
    @GetMapping("/search")
    public Result<List<Attraction>> searchAttractions(@RequestParam String keyword) {
        List<Attraction> attractions = attractionService.search(keyword);
        return Result.success("Searched attractions successfully", attractions);
    }

    public Result<List<Attraction>> searchAttractionsFallback(String keyword, Throwable e) {
        return SentinelFallbackHandler.searchAttractionsFallback(keyword, null);
    }

    @GetMapping("/recommend")
    public Result<List<Attraction>> getRecommendations(@RequestParam Integer cityId,
                                                       @RequestParam(defaultValue = "5") int limit) {
        List<Attraction> attractions = attractionService.getRecommendations(cityId, limit);
        return Result.success("Fetched recommended attractions successfully", attractions);
    }

    @PostMapping
    public Result<Attraction> createAttraction(@RequestBody Attraction attraction) {
        AuthenticatedUserSupport.requireAdmin();
        boolean success = attractionService.save(attraction);
        if (success) {
            return Result.success("Created attraction successfully", attraction);
        }
        return Result.error("Failed to create attraction");
    }

    @PutMapping("/{id}")
    public Result<Attraction> updateAttraction(@PathVariable Integer id, @RequestBody Attraction attraction) {
        AuthenticatedUserSupport.requireAdmin();
        attraction.setId(id);
        boolean success = attractionService.updateById(attraction);
        if (success) {
            return Result.success("Updated attraction successfully", attraction);
        }
        return Result.error("Failed to update attraction");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAttraction(@PathVariable Integer id) {
        AuthenticatedUserSupport.requireAdmin();
        boolean success = attractionService.removeById(id);
        if (success) {
            return Result.success("Deleted attraction successfully", true);
        }
        return Result.error("Failed to delete attraction");
    }


    // ==================== Cursor Pagination & DB Experiment ====================

    @GetMapping("/cursor")
    public Result<CursorPageResult<Attraction>> getAttractionsByCursor(
            @RequestParam Integer cityId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        CursorPageResult<Attraction> result = attractionService.getByCursor(cityId, cursor, size);
        return Result.success("Fetched attractions by cursor successfully", result);
    }

    @GetMapping("/cursor/all")
    public Result<CursorPageResult<Attraction>> getAllAttractionsByCursor(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        CursorPageResult<Attraction> result = attractionService.getAllByCursor(cursor, size);
        return Result.success("Fetched all attractions by cursor successfully", result);
    }

    @GetMapping("/experiment/pagination-compare")
    public Result<Map<String, Object>> comparePagination(
            @RequestParam Integer cityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = attractionService.comparePagination(cityId, page, size);
        return Result.success("Pagination comparison completed", result);
    }
    // ==================== Attraction Detail Extensions ====================

    @GetMapping("/detail/{id}")
    public Result<Attraction> getAttractionDetail(@PathVariable Long id) {
        Attraction detail = attractionDetailService.getAttractionDetail(id);
        return Result.success("Fetched attraction detail successfully", detail);
    }

    @PostMapping("/detail/create")
    public Result<Attraction> createAttractionDetail(@RequestBody Attraction detail) {
        AuthenticatedUserSupport.requireAdmin();
        Attraction result = attractionDetailService.createAttractionDetail(detail);
        return Result.success("Created attraction detail successfully", result);
    }

    @PutMapping("/detail/update/{id}")
    public Result<Attraction> updateAttractionDetail(@PathVariable Long id, @RequestBody Attraction detail) {
        AuthenticatedUserSupport.requireAdmin();
        Attraction result = attractionDetailService.updateAttractionDetail(id, detail);
        return Result.success("Updated attraction detail successfully", result);
    }

    @DeleteMapping("/detail/delete/{id}")
    public Result<Boolean> deleteAttractionDetail(@PathVariable Long id) {
        AuthenticatedUserSupport.requireAdmin();
        boolean result = attractionDetailService.deleteAttractionDetail(id);
        return Result.success("Deleted attraction detail successfully", result);
    }

    // ==================== Paginated Query APIs ====================

    @GetMapping("/city/{cityId}/page")
    public Result<List<Attraction>> getAttractionsByCityWithPage(@PathVariable Integer cityId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        List<Attraction> attractions = attractionDetailService.getAttractionsByCity(cityId, page, size);
        return Result.success("Fetched paged attractions successfully", attractions);
    }

    @GetMapping("/search/page")
    public Result<List<Attraction>> searchAttractionsWithPage(@RequestParam String keyword,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        List<Attraction> attractions = attractionDetailService.searchAttractions(keyword, page, size);
        return Result.success("Searched attractions successfully", attractions);
    }

    @GetMapping("/images/{id}")
    public Result<List<String>> getAttractionImages(@PathVariable Long id) {
        List<String> images = attractionDetailService.getAttractionImages(id);
        return Result.success("Fetched attraction images successfully", images);
    }

    @GetMapping("/reviews/{id}")
    public Result<List<Map<String, Object>>> getAttractionReviews(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        List<Map<String, Object>> reviews = attractionDetailService.getAttractionReviews(id, page, size);
        return Result.success("Fetched attraction reviews successfully", reviews);
    }

    @GetMapping("/rating-statistics/{id}")
    public Result<Map<String, Object>> getRatingStatistics(@PathVariable Long id) {
        Map<String, Object> statistics = attractionDetailService.getRatingStatistics(id);
        return Result.success("Fetched attraction rating statistics successfully", statistics);
    }

    @GetMapping("/similar/{id}")
    public Result<List<Attraction>> getSimilarAttractions(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "5") int limit) {
        List<Attraction> similar = attractionDetailService.getSimilarAttractions(id, limit);
        return Result.success("Fetched similar attractions successfully", similar);
    }

    @GetMapping("/{id}/nearby")
    public Result<List<Map<String, Object>>> getNearby(@PathVariable Long id,
                                                       @RequestParam(required = false) String serviceType) {  // serviceType 已废弃，返回周边景点
        int limit = 5;
        List<Map<String, Object>> services = attractionDetailService.getNearbyAttractions(id.intValue(), limit);
        return Result.success("Fetched nearby attractions successfully", services);
    }

    @PostMapping("/{id}/review")
    public Result<Map<String, Object>> submitReview(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> params) {
        int rating = params.get("rating") instanceof Number n ? n.intValue() : 5;
        String content = params.get("content") == null ? "" : params.get("content").toString();
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        Map<String, Object> review = attractionDetailService.saveAttractionReview(
                id.intValue(), userId, rating, content);
        return Result.success("Review submitted successfully", review);
    }
    @PostMapping("/increment-views/{id}")
    public Result<Boolean> incrementViews(@PathVariable Long id) {
        boolean result = attractionDetailService.incrementViews(id);
        return Result.success("Incremented view count successfully", result);
    }

    @PutMapping("/batch-update")
    public Result<Boolean> batchUpdateAttractions(@RequestBody List<Attraction> attractions) {
        AuthenticatedUserSupport.requireAdmin();
        boolean result = attractionDetailService.batchUpdateAttractions(attractions);
        return Result.success("Batch update completed successfully", result);
    }
}
