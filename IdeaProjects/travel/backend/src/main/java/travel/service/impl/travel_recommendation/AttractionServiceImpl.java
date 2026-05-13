package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import travel.entity.travel_recommendation.Attraction;
import travel.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.service.travel_recommendation.AttractionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction> implements AttractionService {

    private final AttractionMapper attractionMapper;

    public AttractionServiceImpl(AttractionMapper attractionMapper) {
        this.attractionMapper = attractionMapper;
    }

    @Override
    public List<Attraction> getByCityId(Integer cityId) {
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attraction::getCityId, cityId);
        return list(queryWrapper);
    }

    @Override
    public List<Attraction> getByCityIdAndType(Integer cityId, String type) {
        return attractionMapper.selectAttractionPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 100),
                cityId != null ? cityId.longValue() : null,
                type
        ).getRecords();
    }

    @Override
    public Attraction getByIdUsingMapper(Integer attractionId) {
        return attractionMapper.selectLatLngById(attractionId.longValue());
    }

    @Override
    public List<Attraction> getEnableAndSyncOpenAttractions() {
        return attractionMapper.selectEnableAndSyncOpenAttractions();
    }

    @Override
    public List<Attraction> getTopRated(Integer cityId, int limit) {
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attraction::getCityId, cityId);
        queryWrapper.orderByDesc(Attraction::getRating);
        queryWrapper.last("LIMIT " + limit);
        return list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> search(Integer cityId, String keyword) {
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attraction::getCityId, cityId);
        queryWrapper.and(wrapper -> {
            wrapper.like(Attraction::getName, keyword)
                   .or().like(Attraction::getDescription, keyword)
                   .or().like(Attraction::getAddress, keyword);
        });
        queryWrapper.last("LIMIT 20");
        
        List<Attraction> attractions = list(queryWrapper);
        return attractions.stream()
                .map(attraction -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", attraction.getId());
                    map.put("name", attraction.getName());
                    map.put("description", attraction.getDescription());
                    map.put("address", attraction.getAddress());
                    map.put("rating", attraction.getRating());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Attraction> getRecommendations(Integer cityId, int limit) {
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attraction::getCityId, cityId);
        queryWrapper.orderByDesc(Attraction::getRating);
        queryWrapper.last("LIMIT " + limit);
        return list(queryWrapper);
    }

    @Override
    public List<Attraction> search(String keyword) {
        LambdaQueryWrapper<Attraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> {
            wrapper.like(Attraction::getName, keyword)
                   .or().like(Attraction::getDescription, keyword)
                   .or().like(Attraction::getAddress, keyword);
        });
        queryWrapper.last("LIMIT 20");
        return list(queryWrapper);
    }
}