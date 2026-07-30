package travel.attraction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import travel.common.entity.travel_recommendation.City;
import travel.common.mapper.travel_recommendation_mapper.CityMapper;
import travel.common.utils.Result;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityMapper cityMapper;

    /**
     * 获取所有城市列表
     * GET /api/cities
     */
    @GetMapping
    public Result<List<City>> getAllCities() {
        try {
            List<City> cities = cityMapper.selectList(null);
            return Result.success("查询城市列表成功", cities);
        } catch (Exception e) {
            log.error("查询城市列表失败: {}", e.getMessage(), e);
            return Result.error("查询城市列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取城市
     * GET /api/cities/{id}
     */
    @GetMapping("/{id}")
    public Result<City> getCityById(@PathVariable Integer id) {
        try {
            City city = cityMapper.selectById(id);
            if (city == null) {
                return Result.error("城市不存在");
            }
            return Result.success("查询城市成功", city);
        } catch (Exception e) {
            log.error("查询城市失败: id={}, error={}", id, e.getMessage());
            return Result.error("查询城市失败: " + e.getMessage());
        }
    }
}
