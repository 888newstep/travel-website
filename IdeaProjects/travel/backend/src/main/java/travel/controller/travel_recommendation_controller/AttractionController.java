package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.Attraction;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;

    @GetMapping
    public Result<List<Attraction>> getAttractions() {
        List<Attraction> attractions = attractionService.list();
        return Result.success("获取景点列表成功", attractions);
    }

    @GetMapping("/{id}")
    public Result<Attraction> getAttraction(@PathVariable Integer id) {
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            return Result.success("获取景点详情成功", attraction);
        }
        return Result.error("景点不存在");
    }

    @GetMapping("/city/{cityId}")
    public Result<List<Attraction>> getAttractionsByCity(@PathVariable Integer cityId) {
        List<Attraction> attractions = attractionService.getByCityId(cityId);
        return Result.success("获取城市景点成功", attractions);
    }

    @GetMapping("/search")
    public Result<List<Attraction>> searchAttractions(@RequestParam String keyword) {
        List<Attraction> attractions = attractionService.search(keyword);
        return Result.success("搜索景点成功", attractions);
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
}
