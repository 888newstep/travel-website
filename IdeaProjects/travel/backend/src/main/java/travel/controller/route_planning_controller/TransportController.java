package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Transport;
import travel.service.route_planning.TransportService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交通管理控制器
 * 处理交通工具、交通路线和交通信息管理
 */
@RestController
@RequestMapping("/transport")
@RequiredArgsConstructor
public class TransportController {

    private static final Logger log = LoggerFactory.getLogger(TransportController.class);

    private final TransportService transportService;

    /**
     * 添加交通工具
     * POST /api/transport/add
     */
    @PostMapping("/add")
    public Result<Transport> addTransport(@RequestBody Transport transport) {
        try {
            log.info("添加交通工具请求: type={}, name={}", transport.getType(), transport.getName());
            Transport result = transportService.addTransport(transport);
            return Result.success("添加交通工具成功", result);
        } catch (Exception e) {
            log.error("添加交通工具失败: error={}", e.getMessage());
            return Result.error("添加交通工具失败: " + e.getMessage());
        }
    }

    /**
     * 更新交通工具信息
     * PUT /api/transport/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<Transport> updateTransport(@PathVariable Long id, @RequestBody Transport transport) {
        try {
            log.info("更新交通工具信息请求: id={}", id);
            Transport result = transportService.updateTransport(id, transport);
            return Result.success("更新交通工具成功", result);
        } catch (Exception e) {
            log.error("更新交通工具信息失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新交通工具失败: " + e.getMessage());
        }
    }

    /**
     * 删除交通工具
     * DELETE /api/transport/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteTransport(@PathVariable Long id) {
        try {
            log.info("删除交通工具请求: id={}", id);
            boolean result = transportService.deleteTransport(id);
            return Result.success("删除交通工具成功", result);
        } catch (Exception e) {
            log.error("删除交通工具失败: id={}, error={}", id, e.getMessage());
            return Result.error("删除交通工具失败: " + e.getMessage());
        }
    }

    /**
     * 获取交通工具详情
     * GET /api/transport/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result<Transport> getTransportDetail(@PathVariable Long id) {
        try {
            log.info("获取交通工具详情请求: id={}", id);
            Transport transport = transportService.getTransportDetail(id);
            return Result.success("获取详情成功", transport);
        } catch (Exception e) {
            log.error("获取交通工具详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取交通工具列表
     * GET /api/transport/list
     */
    @GetMapping("/list")
    public Result<List<Transport>> getTransportList(@RequestParam(required = false) String type,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取交通工具列表请求: type={}, page={}, size={}", type, page, size);
            List<Transport> transports = transportService.getTransportList(type, page, size);
            return Result.success("获取列表成功", transports);
        } catch (Exception e) {
            log.error("获取交通工具列表失败: error={}", e.getMessage());
            return Result.error("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索交通工具
     * GET /api/transport/search
     */
    @GetMapping("/search")
    public Result<List<Transport>> searchTransports(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("搜索交通工具请求: keyword={}", keyword);
            List<Transport> transports = transportService.searchTransports(keyword, page, size);
            return Result.success("搜索成功", transports);
        } catch (Exception e) {
            log.error("搜索交通工具失败: keyword={}, error={}", keyword, e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取交通工具统计信息
     * GET /api/transport/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getTransportStatistics() {
        try {
            log.info("获取交通工具统计信息请求");
            Map<String, Object> statistics = transportService.getTransportStatistics();
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取交通工具统计信息失败: error={}", e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取交通路线
     * POST /api/transport/route
     */
    @PostMapping("/route")
    public Result<Map<String, Object>> getTransportRoute(@RequestBody Map<String, Object> routeRequest) {
        try {
            log.info("获取交通路线请求: start={}, end={}", routeRequest.get("start"), routeRequest.get("end"));
            Map<String, Object> route = transportService.getTransportRoute(routeRequest);
            return Result.success("获取路线成功", route);
        } catch (Exception e) {
            log.error("获取交通路线失败: error={}", e.getMessage());
            return Result.error("获取路线失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时交通信息
     * GET /api/transport/realtime
     */
    @GetMapping("/realtime")
    public Result<Map<String, Object>> getRealtimeTransportInfo(@RequestParam String location,
                                                                 @RequestParam String transportType) {
        try {
            log.info("获取实时交通信息请求: location={}, transportType={}", location, transportType);
            Map<String, Object> info = transportService.getRealtimeTransportInfo(location, transportType);
            return Result.success("获取实时信息成功", info);
        } catch (Exception e) {
            log.error("获取实时交通信息失败: error={}", e.getMessage());
            return Result.error("获取实时信息失败: " + e.getMessage());
        }
    }

    /**
     * 批量添加交通工具
     * POST /api/transport/batch-add
     */
    @PostMapping("/batch-add")
    public Result<List<Transport>> batchAddTransports(@RequestBody List<Transport> transports) {
        try {
            log.info("批量添加交通工具请求: count={}", transports.size());
            List<Transport> result = transportService.batchAddTransports(transports);
            return Result.success("批量添加成功", result);
        } catch (Exception e) {
            log.error("批量添加交通工具失败: error={}", e.getMessage());
            return Result.error("批量添加失败: " + e.getMessage());
        }
    }
}
