package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.Attraction;
import travel.service.travel_recommendation.AttractionDetailService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 景点详情控制器
 * 处理景点详细信息的展示和管理
 */
@Slf4j
@RestController
@RequestMapping("/attraction-detail")
@RequiredArgsConstructor
public class AttractionDetailController {

    private final AttractionDetailService attractionDetailService;

    /**
     * 获取景点详情
     * GET /api/attraction-detail/{id}
     */
    @GetMapping("/{id}")
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

    /**
     * 创建景点详情
     * POST /api/attraction-detail/create
     */
    @PostMapping("/create")
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

    /**
     * 更新景点详情
     * PUT /api/attraction-detail/update/{id}
     */
    @PutMapping("/update/{id}")
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

    /**
     * 删除景点详情
     * DELETE /api/attraction-detail/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
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

    /**
     * 按城市查询景点详情
     * GET /api/attraction-detail/city/{cityId}
     */
    @GetMapping("/city/{cityId}")
    public Result<List<Attraction>> getAttractionsByCity(@PathVariable Integer cityId,
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

    /**
     * 搜索景点
     * GET /api/attraction-detail/search
     */
    @GetMapping("/search")
    public Result<List<Attraction>> searchAttractions(@RequestParam String keyword,
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

    /**
     * 获取景点图片
     * GET /api/attraction-detail/images/{id}
     */
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

    /**
     * 获取景点评论
     * GET /api/attraction-detail/reviews/{id}
     */
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

    /**
     * 获取景点评分统计
     * GET /api/attraction-detail/rating-statistics/{id}
     */
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

    /**
     * 获取相似景点推荐
     * GET /api/attraction-detail/similar/{id}
     */
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

    /**
     * 更新景点浏览量
     * POST /api/attraction-detail/increment-views/{id}
     */
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

    /**
     * 批量更新景点详情
     * PUT /api/attraction-detail/batch-update
     */
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
