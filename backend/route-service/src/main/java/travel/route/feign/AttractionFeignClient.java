package travel.route.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.Result;

import java.util.List;

@FeignClient(name = "attraction-service", path = "/api/attractions")
public interface AttractionFeignClient {

    @GetMapping("/{id}")
    Result<Attraction> getById(@PathVariable Integer id);

    @GetMapping("/city/{cityId}")
    Result<List<Attraction>> getByCityId(@PathVariable Integer cityId);

    @GetMapping("/search")
    Result<List<Attraction>> search(@RequestParam String keyword);
}