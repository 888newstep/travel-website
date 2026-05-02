package travel.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import travel.entity.route_planning.Transport;
import travel.mapper.route_planning_mapper.TransportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 交通方式Repository接口
 */
@Repository
public class TransportRepository {

    @Autowired
    private TransportMapper transportMapper;

    /**
     * 保存交通方式
     */
    public Transport save(Transport transport) {
        transportMapper.insert(transport);
        return transport;
    }

    /**
     * 更新交通方式
     */
    public boolean update(Transport transport) {
        return transportMapper.updateById(transport) > 0;
    }

    /**
     * 根据ID删除交通方式
     */
    public boolean deleteById(Long id) {
        return transportMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询交通方式
     */
    public Optional<Transport> findById(Long id) {
        Transport transport = transportMapper.selectById(id);
        return Optional.ofNullable(transport);
    }

    /**
     * 查询所有交通方式
     */
    public List<Transport> findAll() {
        return transportMapper.selectList(null);
    }

    /**
     * 根据条件查询交通方式
     */
    public List<Transport> findByCondition(QueryWrapper<Transport> queryWrapper) {
        return transportMapper.selectList(queryWrapper);
    }

    /**
     * 分页查询交通方式
     */
    public Page<Transport> findByPage(Page<Transport> page, QueryWrapper<Transport> queryWrapper) {
        return transportMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据类型查询交通方式
     */
    public List<Transport> findByType(String type) {
        QueryWrapper<Transport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", type);
        return transportMapper.selectList(queryWrapper);
    }

    /**
     * 根据城市ID查询交通方式
     */
    public List<Transport> findByCityId(Long cityId) {
        QueryWrapper<Transport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId);
        return transportMapper.selectList(queryWrapper);
    }
}
