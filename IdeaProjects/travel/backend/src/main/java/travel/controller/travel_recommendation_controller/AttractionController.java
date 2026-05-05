package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.Attraction;
import travel.service.travel_recommendation.AttractionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;

    @GetMapping
    public List<Attraction> getAttractions() {
        return attractionService.list();
    }

    @GetMapping("/{id}")
    public Attraction getAttraction(@PathVariable Integer id) {
        return attractionService.getById(id);
    }

    @GetMapping("/city/{cityId}")
    public List<Attraction> getAttractionsByCity(@PathVariable Integer cityId) {
        return attractionService.getByCityId(cityId);
    }

    @GetMapping("/search")
    public List<Attraction> searchAttractions(@RequestParam String keyword) {
        return attractionService.search(keyword);
    }

    @GetMapping("/recommend")
    public List<Attraction> getRecommendations(@RequestParam Integer cityId,
                                               @RequestParam(defaultValue = "5") int limit) {
        return attractionService.getRecommendations(cityId, limit);
    }

    @PostMapping
    public boolean createAttraction(@RequestBody Attraction attraction) {
        return attractionService.save(attraction);
    }

    @PutMapping("/{id}")
    public boolean updateAttraction(@PathVariable Integer id, @RequestBody Attraction attraction) {
        attraction.setId(id);
        return attractionService.updateById(attraction);
    }

    @DeleteMapping("/{id}")
    public boolean deleteAttraction(@PathVariable Integer id) {
        return attractionService.removeById(id);
    }
}