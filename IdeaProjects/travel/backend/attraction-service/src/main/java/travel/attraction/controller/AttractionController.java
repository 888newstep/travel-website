package travel.attraction.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.config.SentinelFallbackHandler;
import travel.common.entity.travel_recommendation.Attraction;
import travel.attraction.service.AttractionDetailService;
import travel.attraction.service.AttractionService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import travel.common.vo.CursorPageResult;


@Slf4j
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
        boolean success = attractionService.save(attraction);
        if (success) {
            return Result.success("Created attraction successfully", attraction);
        }
        return Result.error("Failed to create attraction");
    }

    @PutMapping("/{id}")
    public Result<Attraction> updateAttraction(@PathVariable Integer id, @RequestBody Attraction attraction) {
        attraction.setId(id);
        boolean success = attractionService.updateById(attraction);
        if (success) {
            return Result.success("Updated attraction successfully", attraction);
        }
        return Result.error("Failed to update attraction");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAttraction(@PathVariable Integer id) {
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
        try {
            log.info("Get attraction detail request: id={}", id);
            Attraction detail = attractionDetailService.getAttractionDetail(id);
            return Result.success("Fetched attraction detail successfully", detail);
        } catch (Exception e) {
            log.error("Failed to get attraction detail: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to get attraction detail: " + e.getMessage());
        }
    }

    @PostMapping("/detail/create")
    public Result<Attraction> createAttractionDetail(@RequestBody Attraction detail) {
        try {
            log.info("Create attraction detail request: name={}", detail.getName());
            Attraction result = attractionDetailService.createAttractionDetail(detail);
            return Result.success("Created attraction detail successfully", result);
        } catch (Exception e) {
            log.error("Failed to create attraction detail: error={}", e.getMessage());
            return Result.error("Failed to create attraction detail: " + e.getMessage());
        }
    }

    @PutMapping("/detail/update/{id}")
    public Result<Attraction> updateAttractionDetail(@PathVariable Long id, @RequestBody Attraction detail) {
        try {
            log.info("Update attraction detail request: id={}", id);
            Attraction result = attractionDetailService.updateAttractionDetail(id, detail);
            return Result.success("Updated attraction detail successfully", result);
        } catch (Exception e) {
            log.error("Failed to update attraction detail: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to update attraction detail: " + e.getMessage());
        }
    }

    @DeleteMapping("/detail/delete/{id}")
    public Result<Boolean> deleteAttractionDetail(@PathVariable Long id) {
        try {
            log.info("Delete attraction detail request: id={}", id);
            boolean result = attractionDetailService.deleteAttractionDetail(id);
            return Result.success("Deleted attraction detail successfully", result);
        } catch (Exception e) {
            log.error("Failed to delete attraction detail: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to delete attraction detail: " + e.getMessage());
        }
    }

    // ==================== Paginated Query APIs ====================

    @GetMapping("/city/{cityId}/page")
    public Result<List<Attraction>> getAttractionsByCityWithPage(@PathVariable Integer cityId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Paged city attractions request: cityId={}", cityId);
            List<Attraction> attractions = attractionDetailService.getAttractionsByCity(cityId, page, size);
            return Result.success("Fetched paged attractions successfully", attractions);
        } catch (Exception e) {
            log.error("Failed to fetch paged city attractions: cityId={}, error={}", cityId, e.getMessage());
            return Result.error("Failed to fetch paged attractions: " + e.getMessage());
        }
    }

    @GetMapping("/search/page")
    public Result<List<Attraction>> searchAttractionsWithPage(@RequestParam String keyword,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Paged attraction search request: keyword={}", keyword);
            List<Attraction> attractions = attractionDetailService.searchAttractions(keyword, page, size);
            return Result.success("Searched attractions successfully", attractions);
        } catch (Exception e) {
            log.error("Failed to search attractions: keyword={}, error={}", keyword, e.getMessage());
            return Result.error("Failed to search attractions: " + e.getMessage());
        }
    }

    @GetMapping("/images/{id}")
    public Result<List<String>> getAttractionImages(@PathVariable Long id) {
        try {
            log.info("Get attraction images request: id={}", id);
            List<String> images = attractionDetailService.getAttractionImages(id);
            return Result.success("Fetched attraction images successfully", images);
        } catch (Exception e) {
            log.error("Failed to get attraction images: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to get attraction images: " + e.getMessage());
        }
    }

    @GetMapping("/reviews/{id}")
    public Result<List<Map<String, Object>>> getAttractionReviews(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Get attraction reviews request: id={}", id);
            List<Map<String, Object>> reviews = attractionDetailService.getAttractionReviews(id, page, size);
            return Result.success("Fetched attraction reviews successfully", reviews);
        } catch (Exception e) {
            log.error("Failed to get attraction reviews: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to get attraction reviews: " + e.getMessage());
        }
    }

    @GetMapping("/rating-statistics/{id}")
    public Result<Map<String, Object>> getRatingStatistics(@PathVariable Long id) {
        try {
            log.info("Get attraction rating statistics request: id={}", id);
            Map<String, Object> statistics = attractionDetailService.getRatingStatistics(id);
            return Result.success("Fetched attraction rating statistics successfully", statistics);
        } catch (Exception e) {
            log.error("Failed to get attraction rating statistics: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to get attraction rating statistics: " + e.getMessage());
        }
    }

    @GetMapping("/similar/{id}")
    public Result<List<Attraction>> getSimilarAttractions(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("Get similar attractions request: id={}", id);
            List<Attraction> similar = attractionDetailService.getSimilarAttractions(id, limit);
            return Result.success("Fetched similar attractions successfully", similar);
        } catch (Exception e) {
            log.error("Failed to get similar attractions: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to get similar attractions: " + e.getMessage());
        }
    }

    @PostMapping("/increment-views/{id}")
    public Result<Boolean> incrementViews(@PathVariable Long id) {
        try {
            log.info("Increment attraction views request: id={}", id);
            boolean result = attractionDetailService.incrementViews(id);
            return Result.success("Incremented view count successfully", result);
        } catch (Exception e) {
            log.error("Failed to increment attraction views: id={}, error={}", id, e.getMessage());
            return Result.error("Failed to increment views: " + e.getMessage());
        }
    }

    @PutMapping("/batch-update")
    public Result<Boolean> batchUpdateAttractions(@RequestBody List<Attraction> attractions) {
        try {
            log.info("Batch update attractions request: count={}", attractions.size());
            boolean result = attractionDetailService.batchUpdateAttractions(attractions);
            return Result.success("Batch update completed successfully", result);
        } catch (Exception e) {
            log.error("Failed to batch update attractions: error={}", e.getMessage());
            return Result.error("Failed to batch update attractions: " + e.getMessage());
        }
    }
}



