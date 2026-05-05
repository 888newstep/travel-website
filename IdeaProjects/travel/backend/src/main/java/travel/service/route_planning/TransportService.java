package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.route_planning.Transport;
import travel.entity.route_planning.TransportType;

import java.util.List;
import java.util.Map;

public interface TransportService extends IService<Transport> {

    /**
     * 根据交通类型获取交通工具列表
     */
    List<Transport> getByType(TransportType type);

    /**
     * 获取所有交通工具类型
     */
    List<TransportType> getAllTypes();

    /**
     * 计算两个景点之间的交通方案
     */
    List<Transport> calculateTransportOptions(Integer fromAttractionId, Integer toAttractionId);

    /**
     * 根据偏好获取推荐的交通工具
     */
    Transport getRecommendedTransport(Integer fromAttractionId, Integer toAttractionId, String preference);

    /**
     * 获取实时交通数据
     */
    Map<String, Object> getRealTimeTrafficData(String fromLocation, String toLocation);

    // 以下是Controller中使用的方法

    /**
     * 添加交通工具
     * @param transport 交通工具
     * @return 添加的交通工具
     */
    Transport addTransport(Transport transport);

    /**
     * 更新交通工具
     * @param id ID
     * @param transport 交通工具
     * @return 更新后的交通工具
     */
    Transport updateTransport(Long id, Transport transport);

    /**
     * 删除交通工具
     * @param id ID
     * @return 是否成功
     */
    boolean deleteTransport(Long id);

    /**
     * 获取交通工具详情
     * @param id ID
     * @return 交通工具
     */
    Transport getTransportDetail(Long id);

    /**
     * 获取交通工具列表
     * @param type 类型
     * @param page 页码
     * @param size 每页数量
     * @return 交通工具列表
     */
    List<Transport> getTransportList(String type, int page, int size);

    /**
     * 搜索交通工具
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 交通工具列表
     */
    List<Transport> searchTransports(String keyword, int page, int size);

    /**
     * 获取交通工具统计
     * @return 统计数据
     */
    Map<String, Object> getTransportStatistics();

    /**
     * 获取交通工具路线
     * @param routeRequest 路线请求
     * @return 路线
     */
    Map<String, Object> getTransportRoute(Map<String, Object> routeRequest);

    /**
     * 获取实时交通信息
     * @param location 位置
     * @param transportType 交通类型
     * @return 实时交通信息
     */
    Map<String, Object> getRealtimeTransportInfo(String location, String transportType);

    /**
     * 批量添加交通工具
     * @param transports 交通工具列表
     * @return 添加的交通工具列表
     */
    List<Transport> batchAddTransports(List<Transport> transports);
}
