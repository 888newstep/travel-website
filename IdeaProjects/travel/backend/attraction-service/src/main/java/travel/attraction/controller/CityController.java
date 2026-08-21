package travel.attraction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import travel.common.enums.ErrorCodeEnum;
import travel.common.entity.travel_recommendation.City;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_recommendation_mapper.CityMapper;
import travel.common.utils.Result;

import java.util.List;

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
        List<City> cities = cityMapper.selectList(null);
        return Result.success("查询城市列表成功", cities);
    }

    /**
     * 根据ID获取城市
     * GET /api/cities/{id}
     */
    @GetMapping("/{id}")
    public Result<City> getCityById(@PathVariable Integer id) {
        City city = cityMapper.selectById(id);
        if (city == null) {
            throw new BusinessException(ErrorCodeEnum.CITY_NOT_EXIST);
        }
        return Result.success("查询城市成功", city);
    }
}
