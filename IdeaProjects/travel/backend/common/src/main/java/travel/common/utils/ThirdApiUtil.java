package travel.common.utils;

import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class ThirdApiUtil {

    private static final Logger log = LoggerFactory.getLogger(ThirdApiUtil.class);

    /**
     * 调用第三方API获取实时数据
     */
    public List<AttractionRealtimeStatus> getRealtimeData(List<AttractionRealtimeStatus> statusList) {
        List<AttractionRealtimeStatus> updatedStatusList = new ArrayList<>();
        
        try {
            // 模拟调用第三方API
            for (AttractionRealtimeStatus status : statusList) {
                AttractionRealtimeStatus updatedStatus = new AttractionRealtimeStatus();
                updatedStatus.setAttractionId(status.getAttractionId());
                
                // 模拟天气数据
                String[] weathers = {"晴", "多云", "小雨", "中雨", "大雨", "暴雨"};
                updatedStatus.setWeather(weathers[new Random().nextInt(weathers.length)]);
                
                // 模拟温度数据
                updatedStatus.setTemperature(new Random().nextInt(35) - 5);
                
                // 模拟人流数据
                updatedStatus.setCrowdCount(new Random().nextInt(1500) + 100);
                
                // 计算人流等级
                int crowdCount = updatedStatus.getCrowdCount();
                if (crowdCount < 300) {
                    updatedStatus.setCrowdLevel(1);
                } else if (crowdCount < 600) {
                    updatedStatus.setCrowdLevel(2);
                } else if (crowdCount < 900) {
                    updatedStatus.setCrowdLevel(3);
                } else if (crowdCount < 1200) {
                    updatedStatus.setCrowdLevel(4);
                } else {
                    updatedStatus.setCrowdLevel(5);
                }
                
                updatedStatusList.add(updatedStatus);
            }
            
            log.info("成功获取 {} 个景点的实时数据", updatedStatusList.size());
            
        } catch (Exception e) {
            log.error("调用第三方API获取实时数据失败: {}", e.getMessage(), e);
        }
        
        return updatedStatusList;
    }

    /**
     * 获取单个景点的实时数据
     */
    public AttractionRealtimeStatus getRealtimeData(Long attractionId) {
        AttractionRealtimeStatus status = new AttractionRealtimeStatus();
        status.setAttractionId(attractionId);
        
        try {
            // 模拟调用第三方API
            String[] weathers = {"晴", "多云", "小雨", "中雨", "大雨", "暴雨"};
            status.setWeather(weathers[new Random().nextInt(weathers.length)]);
            
            status.setTemperature(new Random().nextInt(35) - 5);
            status.setCrowdCount(new Random().nextInt(1500) + 100);
            
            int crowdCount = status.getCrowdCount();
            if (crowdCount < 300) {
                status.setCrowdLevel(1);
            } else if (crowdCount < 600) {
                status.setCrowdLevel(2);
            } else if (crowdCount < 900) {
                status.setCrowdLevel(3);
            } else if (crowdCount < 1200) {
                status.setCrowdLevel(4);
            } else {
                status.setCrowdLevel(5);
            }
            
        } catch (Exception e) {
            log.error("获取景点 {} 实时数据失败: {}", attractionId, e.getMessage(), e);
        }
        
        return status;
    }

    /**
     * 获取交通实时数据
     */
    public java.util.Map<String, Object> getTransportData(String fromLocation, String toLocation) {
        try {
            // 模拟调用第三方API获取交通实时数据
            java.util.Map<String, Object> transportData = new java.util.HashMap<>();
            transportData.put("status", "success");
            transportData.put("fromLocation", fromLocation);
            transportData.put("toLocation", toLocation);
            
            // 模拟拥堵等级
            String[] congestionLevels = {"畅通", "轻度拥堵", "中度拥堵", "严重拥堵"};
            transportData.put("congestionLevel", congestionLevels[new Random().nextInt(congestionLevels.length)]);
            
            // 模拟预计时间
            transportData.put("estimatedTime", new Random().nextInt(60) + 5); // 分钟
            
            // 模拟距离
            transportData.put("distance", new Random().nextDouble() * 50 + 1); // 公里
            
            // 模拟交通方式选项
            java.util.List<java.util.Map<String, Object>> transportOptions = new java.util.ArrayList<>();
            
            // 公交选项
            java.util.Map<String, Object> busOption = new java.util.HashMap<>();
            busOption.put("type", "公交");
            busOption.put("price", new Random().nextInt(10) + 1); // 元
            busOption.put("time", new Random().nextInt(40) + 20); // 分钟
            busOption.put("transfers", new Random().nextInt(3)); // 换乘次数
            transportOptions.add(busOption);
            
            // 地铁选项
            java.util.Map<String, Object> subwayOption = new java.util.HashMap<>();
            subwayOption.put("type", "地铁");
            subwayOption.put("price", new Random().nextInt(15) + 3); // 元
            subwayOption.put("time", new Random().nextInt(30) + 10); // 分钟
            subwayOption.put("transfers", new Random().nextInt(2)); // 换乘次数
            transportOptions.add(subwayOption);
            
            // 打车选项
            java.util.Map<String, Object> taxiOption = new java.util.HashMap<>();
            taxiOption.put("type", "打车");
            taxiOption.put("price", new Random().nextInt(100) + 20); // 元
            taxiOption.put("time", new Random().nextInt(40) + 10); // 分钟
            taxiOption.put("transfers", 0); // 换乘次数
            transportOptions.add(taxiOption);
            
            transportData.put("transportOptions", transportOptions);
            
            // 模拟实时路况
            java.util.List<String> roadConditions = new java.util.ArrayList<>();
            roadConditions.add("前方500米有施工，请注意减速");
            roadConditions.add("前方2公里处交通信号灯故障，请谨慎驾驶");
            roadConditions.add("建议走备选路线，可节省约10分钟");
            transportData.put("roadConditions", roadConditions);
            
            log.info("获取交通实时数据成功: from={}, to={}", fromLocation, toLocation);
            return transportData;
        } catch (Exception e) {
            log.error("获取交通实时数据失败: from={}, to={}, error={}", fromLocation, toLocation, e.getMessage());
            java.util.Map<String, Object> errorData = new java.util.HashMap<>();
            errorData.put("status", "failure");
            errorData.put("message", "获取交通实时数据失败: " + e.getMessage());
            return errorData;
        }
    }

    /**
     * 获取实时交通信息
     */
    public java.util.Map<String, Object> getRealTimeTrafficInfo(String route) {
        try {
            // 模拟调用第三方API获取实时交通信息
            java.util.Map<String, Object> trafficInfo = new java.util.HashMap<>();
            trafficInfo.put("status", "success");
            trafficInfo.put("congestionLevel", "moderate");
            trafficInfo.put("estimatedTime", 15); // 分钟
            trafficInfo.put("distance", 5.2); // 公里
            trafficInfo.put("suggestedRoute", "建议走主干道");
            
            log.info("获取实时交通信息成功: route={}", route);
            return trafficInfo;
        } catch (Exception e) {
            log.error("获取实时交通信息失败: route={}, error={}", route, e.getMessage());
            throw new RuntimeException("获取实时交通信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时景点状态
     */
    public java.util.Map<String, Object> getRealTimeAttractionStatus(String attractionId) {
        try {
            // 模拟调用第三方API获取实时景点状态
            java.util.Map<String, Object> attractionStatus = new java.util.HashMap<>();
            attractionStatus.put("status", "success");
            attractionStatus.put("crowdLevel", "moderate");
            attractionStatus.put("waitTime", 10); // 分钟
            attractionStatus.put("isOpen", true);
            attractionStatus.put("weather", "晴");
            
            log.info("获取实时景点状态成功: attractionId={}", attractionId);
            return attractionStatus;
        } catch (Exception e) {
            log.error("获取实时景点状态失败: attractionId={}, error={}", attractionId, e.getMessage());
            throw new RuntimeException("获取实时景点状态失败: " + e.getMessage());
        }
    }

    /**
     * 发送短信通知
     */
    public boolean sendSmsNotification(String phone, String templateParam, String templateCode) {
        // 模拟发送短信
        try {
            log.info("发送短信通知到: {}, 模板参数: {}, 模板代码: {}", phone, templateParam, templateCode);
            return true;
        } catch (Exception e) {
            log.error("发送短信通知失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
