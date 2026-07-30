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

@Slf4j
@RestController
@RequestMapping("/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;
    private final AttractionDetailService attractionDetailService;

    // ==================== 基础CRUD ====================

    @GetMapping
    public Result<List<Attraction>> getAttractions() {
        List<Attraction> attractions = attractionService.list();
        return Result.success("获取景点列表成功", attractions);
    }

    @SentinelResource(value = "getAttractionById",
            fallback = "getAttractionByIdFallback", blockHandler = "getAttractionByIdFallback")
    @GetMapping("/{id}")
    public Result<Attraction> getAttraction(@PathVariable Integer id) {
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            return Result.success("获取景点详情成功", attraction);
        }
        return Result.error("景点不存在");
    }

    public Result<Attraction> getAttractionByIdFallback(Integer id, Throwable e) {
        return SentinelFallbackHandler.getAttractionByIdFallback(id, null);
    }

    @SentinelResource(value = "getAttractionsByCity",
            fallback = "getAttractionsByCityFallback", blockHandler = "getAttractionsByCityFallback")
    @GetMapping("/city/{cityId}")
    public Result<List<Attraction>> getAttractionsByCity(@PathVariable Integer cityId) {
        List<Attraction> attractions = attractionService.getByCityId(cityId);
        return Result.success("获取城市景点成功", attractions);
    }

    public Result<List<Attraction>> getAttractionsByCityFallback(Integer cityId, Throwable e) {
        return SentinelFallbackHandler.getAttractionsByCityFallback(cityId, null);
    }

    @SentinelResource(value = "searchAttractions",
            fallback = "searchAttractionsFallback", blockHandler = "searchAttractionsFallback")
    @GetMapping("/search")
    public Result<List<Attraction>> searchAttractions(@RequestParam String keyword) {
        List<Attraction> attractions = attractionService.search(keyword);
        return Result.success("搜索景点成功", attractions);
    }

    public Result<List<Attraction>> searchAttractionsFallback(String keyword, Throwable e) {
        return SentinelFallbackHandler.searchAttractionsFallback(keyword, null);
    }

    @GetMapping("/recommend")
    public Result<List<Attraction>> getRecommendations(@RequestParam Integer cityId,
                                                       @RequestParam(defaultValue = "5") int limit) {
        List<Attraction> attractions = attractionService.getRecommendations(cityId, limit);
        return Result.success("获取推荐景点成功", attractions);
    }

    @PostMapping
    public Result<Attraction> createAttraction(@RequestBody Attraction attraction) {
        boolean success = attractionService.save(attraction);
        if (success) {
            return Result.success("创建景点成功", attraction);
        }
        return Result.error("创建景点失败");
    }

    @PutMapping("/{id}")
    public Result<Attraction> updateAttraction(@PathVariable Integer id, @RequestBody Attraction attraction) {
        attraction.setId(id);
        boolean success = attractionService.updateById(attraction);
        if (success) {
            return Result.success("更新景点成功", attraction);
        }
        return Result.error("更新景点失败");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAttraction(@PathVariable Integer id) {
        boolean success = attractionService.removeById(id);
        if (success) {
            return Result.success("删除景点成功", true);
        }
        return Result.error("删除景点失败");
    }

    // ==================== 详情管理 ====================

    @GetMapping("/detail/{id}")
    public Result<Attraction> getAttractionDetail(@PathVariable Long id) {
        try {
            log.info("获取景点详情请求: id={}", id);
            Attraction detail = attractionDetailService.getAttractionDetail(id);
            return Result.success("获取景点详情成功", detail);
        } catch (Exception e) {
            log.error("获取景点详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取景点详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/detail/create")
    public Result<Attraction> createAttractionDetail(@RequestBody Attraction detail) {
        try {
            log.info("创建景点详情请求: name={}", detail.getName());
            Attraction result = attractionDetailService.createAttractionDetail(detail);
            return Result.success("创建景点详情成功", result);
        } catch (Exception e) {
            log.error("创建景点详情失败: error={}", e.getMessage());
            return Result.error("创建景点详情失败: " + e.getMessage());
        }
    }

    @PutMapping("/detail/update/{id}")
    public Result<Attraction> updateAttractionDetail(@PathVariable Long id, @RequestBody Attraction detail) {
        try {
            log.info("更新景点详情请求: id={}", id);
            Attraction result = attractionDetailService.updateAttractionDetail(id, detail);
            return Result.success("更新景点详情成功", result);
        } catch (Exception e) {
            log.error("更新景点详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新景点详情失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/detail/delete/{id}")
    public Result<Boolean> deleteAttractionDetail(@PathVariable Long id) {
        try {
            log.info("删除景点详情请求: id={}", id);
            boolean result = attractionDetailService.deleteAttractionDetail(id);
            return Result.success("删除景点详情成功", result);
        } catch (Exception e) {
            log.error("删除景点详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("删除景点详情失败: " + e.getMessage());
        }
    }

    // ==================== 扩展功能 ====================

    @GetMapping("/city/{cityId}/page")
    public Result<List<Attraction>> getAttractionsByCityWithPage(@PathVariable Integer cityId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("按城市查询景点详情请求: cityId={}", cityId);
            List<Attraction> attractions = attractionDetailService.getAttractionsByCity(cityId, page, size);
            return Result.success("查询成功", attractions);
        } catch (Exception e) {
            log.error("按城市查询景点详情失败: cityId={}, error={}", cityId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/search/page")
    public Result<List<Attraction>> searchAttractionsWithPage(@RequestParam String keyword,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("搜索景点请求: keyword={}", keyword);
            List<Attraction> attractions = attractionDetailService.searchAttractions(keyword, page, size);
            return Result.success("搜索成功", attractions);
        } catch (Exception e) {
            log.error("搜索景点失败: keyword={}, error={}", keyword, e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @GetMapping("/images/{id}")
    public Result<List<String>> getAttractionImages(@PathVariable Long id) {
        try {
            log.info("获取景点图片请求: id={}", id);
            List<String> images = attractionDetailService.getAttractionImages(id);
            return Result.success("获取图片成功", images);
        } catch (Exception e) {
            log.error("获取景点图片失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取图片失败: " + e.getMessage());
        }
    }

    @GetMapping("/reviews/{id}")
    public Result<List<Map<String, Object>>> getAttractionReviews(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取景点评论请求: id={}", id);
            List<Map<String, Object>> reviews = attractionDetailService.getAttractionReviews(id, page, size);
            return Result.success("获取评论成功", reviews);
        } catch (Exception e) {
            log.error("获取景点评论失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取评论失败: " + e.getMessage());
        }
    }

    @GetMapping("/rating-statistics/{id}")
    public Result<Map<String, Object>> getRatingStatistics(@PathVariable Long id) {
        try {
            log.info("获取景点评分统计请求: id={}", id);
            Map<String, Object> statistics = attractionDetailService.getRatingStatistics(id);
            return Result.success("获取评分统计成功", statistics);
        } catch (Exception e) {
            log.error("获取景点评分统计失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取评分统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/similar/{id}")
    public Result<List<Attraction>> getSimilarAttractions(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似景点推荐请求: id={}", id);
            List<Attraction> similar = attractionDetailService.getSimilarAttractions(id, limit);
            return Result.success("获取相似景点成功", similar);
        } catch (Exception e) {
            log.error("获取相似景点推荐失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取相似景点失败: " + e.getMessage());
        }
    }

    @PostMapping("/increment-views/{id}")
    public Result<Boolean> incrementViews(@PathVariable Long id) {
        try {
            log.info("更新景点浏览量请求: id={}", id);
            boolean result = attractionDetailService.incrementViews(id);
            return Result.success("更新浏览量成功", result);
        } catch (Exception e) {
            log.error("更新景点浏览量失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新浏览量失败: " + e.getMessage());
        }
    }

    @PutMapping("/batch-update")
    public Result<Boolean> batchUpdateAttractions(@RequestBody List<Attraction> attractions) {
        try {
            log.info("批量更新景点详情请求: count={}", attractions.size());
            boolean result = attractionDetailService.batchUpdateAttractions(attractions);
            return Result.success("批量更新成功", result);
        } catch (Exception e) {
            log.error("批量更新景点详情失败: error={}", e.getMessage());
            return Result.error("批量更新失败: " + e.getMessage());
        }
    }
}