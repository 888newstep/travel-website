package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import travel.entity.travel_recommendation.Attraction;
import travel.mapper.travel_recommendation_mapper.AttractionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 景点Repository接口
 */
@Repository
public class AttractionRepository {

    @Autowired
    private AttractionMapper attractionMapper;

    /**
     * 保存景点
     */
    public Attraction save(Attraction attraction) {
        attractionMapper.insert(attraction);
        return attraction;
    }

    /**
     * 更新景点
     */
    public boolean update(Attraction attraction) {
        return attractionMapper.updateById(attraction) > 0;
    }

    /**
     * 根据ID删除景点
     */
    public boolean deleteById(Long id) {
        return attractionMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询景点
     */
    public Optional<Attraction> findById(Long id) {
        Attraction attraction = attractionMapper.selectById(id);
        return Optional.ofNullable(attraction);
    }

    /**
     * 查询所有景点
     */
    public List<Attraction> findAll() {
        return attractionMapper.selectList(null);
    }

    /**
     * 根据条件查询景点
     */
    public List<Attraction> findByCondition(QueryWrapper<Attraction> queryWrapper) {
        return attractionMapper.selectList(queryWrapper);
    }

    /**
     * 分页查询景点
     */
    public Page<Attraction> findByPage(Page<Attraction> page, QueryWrapper<Attraction> queryWrapper) {
        return attractionMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据城市ID查询景点
     */
    public List<Attraction> findByCityId(Long cityId) {
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        return attractionMapper.selectList(queryWrapper);
    }

    /**
     * 根据类型查询景点
     */
    public List<Attraction> findByType(String type) {
        QueryWrapper<Attraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", type);
        return attractionMapper.selectList(queryWrapper);
    }
}
