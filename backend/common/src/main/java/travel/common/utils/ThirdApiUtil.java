package travel.common.utils;

import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ThirdApiUtil {

    private static final Logger log = LoggerFactory.getLogger(ThirdApiUtil.class);
    private final AMapService aMapService;

    public ThirdApiUtil(AMapService aMapService) {
        this.aMapService = aMapService;
    }

    /**
     * 调用第三方API获取实时数据
     */
    public List<AttractionRealtimeStatus> getRealtimeData(List<AttractionRealtimeStatus> statusList) {
        log.error("景点实时数据供应商尚未配置，拒绝生成模拟数据: count={}",
                statusList == null ? 0 : statusList.size());
        throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_SERVICE_ERROR);
    }

    /**
     * 获取单个景点的实时数据
     */
    public AttractionRealtimeStatus getRealtimeData(Long attractionId) {
        log.error("景点实时数据供应商尚未配置，拒绝生成模拟数据: attractionId={}", attractionId);
        throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_SERVICE_ERROR);
    }

    /**
     * 获取交通实时数据
     */
    public Map<String, Object> getTransportData(String fromLocation, String toLocation) {
        double[] origin = parseLocation(fromLocation);
        double[] destination = parseLocation(toLocation);
        Map<String, Object> route = aMapService.drivingRoute(
                origin[0], origin[1], destination[0], destination[1]);
        if (route == null || route.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_FETCH_FAILED);
        }
        Map<String, Object> result = new LinkedHashMap<>(route);
        result.put("fromLocation", fromLocation);
        result.put("toLocation", toLocation);
        result.put("status", "success");
        result.put("source", "amap");
        return result;
    }

    /**
     * 获取实时交通信息
     */
    public Map<String, Object> getRealTimeTrafficInfo(String route) {
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String[] endpoints = route.split(";", -1);
        if (endpoints.length != 2) {
            throw new BusinessException(ErrorCodeEnum.PARAM_FORMAT_ERROR);
        }
        return getTransportData(endpoints[0], endpoints[1]);
    }

    /**
     * 获取实时景点状态
     */
    public java.util.Map<String, Object> getRealTimeAttractionStatus(String attractionId) {
        log.error("景点实时数据供应商尚未配置: attractionId={}", attractionId);
        throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_SERVICE_ERROR);
    }

    /**
     * 发送短信通知
     */
    public boolean sendSmsNotification(String phone, String templateParam, String templateCode) {
        log.info("当前展示项目未启用短信发送: templateCode={}", templateCode);
        throw new BusinessException(ErrorCodeEnum.THIRD_PARTY_SERVICE_ERROR);
    }

    private double[] parseLocation(String location) {
        if (location == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String[] coordinates = location.trim().split(",", -1);
        if (coordinates.length != 2) {
            throw new BusinessException(ErrorCodeEnum.PARAM_FORMAT_ERROR);
        }
        try {
            double longitude = Double.parseDouble(coordinates[0].trim());
            double latitude = Double.parseDouble(coordinates[1].trim());
            if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
                throw new BusinessException(ErrorCodeEnum.PARAM_RANGE_ERROR);
            }
            return new double[]{longitude, latitude};
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCodeEnum.PARAM_FORMAT_ERROR);
        }
    }
}
