package travel.service.travel_realtime;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_realtime.AttractionRealtimeStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeWarnService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeWarnService.class);

    private final AttractionRealtimeStatusService attractionRealtimeStatusService;

    /**
     * 检查景点实时状态并生成预警
     */
    public List<WarnInfo> checkRealtimeStatus() {
        List<WarnInfo> warnInfoList = new ArrayList<>();
        
        try {
            // 1. 获取所有景点的实时状态
            List<AttractionRealtimeStatus> statusList = attractionRealtimeStatusService.list();
            
            // 2. 检查每个景点的状态
            for (AttractionRealtimeStatus status : statusList) {
                List<WarnInfo> warns = checkSingleAttractionStatus(status);
                warnInfoList.addAll(warns);
            }
            
        } catch (Exception e) {
            log.error("检查景点实时状态失败: {}", e.getMessage(), e);
        }
        
        return warnInfoList;
    }

    /**
     * 检查单个景点的实时状态
     */
    private List<WarnInfo> checkSingleAttractionStatus(AttractionRealtimeStatus status) {
        List<WarnInfo> warnInfoList = new ArrayList<>();
        
        // 检查人流预警
        if (status.getCrowdCount() != null && status.getCrowdCount() > 1000) {
            WarnInfo crowdWarn = new WarnInfo();
            crowdWarn.setAttractionId(status.getAttractionId());
            crowdWarn.setWarnType("CROWD");
            crowdWarn.setWarnLevel("HIGH");
            crowdWarn.setWarnMessage("景点人流过多，建议游客避开");
            crowdWarn.setWarnValue(status.getCrowdCount().toString());
            warnInfoList.add(crowdWarn);
        }
        
        // 检查天气预警
        if (status.getWeather() != null) {
            String weather = status.getWeather();
            if (weather.contains("暴雨") || weather.contains("台风") || weather.contains("暴雪")) {
                WarnInfo weatherWarn = new WarnInfo();
                weatherWarn.setAttractionId(status.getAttractionId());
                weatherWarn.setWarnType("WEATHER");
                weatherWarn.setWarnLevel("HIGH");
                weatherWarn.setWarnMessage("景点天气恶劣，建议游客取消行程");
                weatherWarn.setWarnValue(weather);
                warnInfoList.add(weatherWarn);
            } else if (weather.contains("大雨") || weather.contains("大风")) {
                WarnInfo weatherWarn = new WarnInfo();
                weatherWarn.setAttractionId(status.getAttractionId());
                weatherWarn.setWarnType("WEATHER");
                weatherWarn.setWarnLevel("MEDIUM");
                weatherWarn.setWarnMessage("景点天气不佳，建议游客携带雨具");
                weatherWarn.setWarnValue(weather);
                warnInfoList.add(weatherWarn);
            }
        }
        
        // 检查温度预警
        if (status.getTemperature() != null) {
            int temperature = status.getTemperature();
            if (temperature > 35) {
                WarnInfo tempWarn = new WarnInfo();
                tempWarn.setAttractionId(status.getAttractionId());
                tempWarn.setWarnType("TEMPERATURE");
                tempWarn.setWarnLevel("HIGH");
                tempWarn.setWarnMessage("景点温度过高，建议游客做好防暑措施");
                tempWarn.setWarnValue(temperature + "℃");
                warnInfoList.add(tempWarn);
            } else if (temperature < 0) {
                WarnInfo tempWarn = new WarnInfo();
                tempWarn.setAttractionId(status.getAttractionId());
                tempWarn.setWarnType("TEMPERATURE");
                tempWarn.setWarnLevel("HIGH");
                tempWarn.setWarnMessage("景点温度过低，建议游客做好保暖措施");
                tempWarn.setWarnValue(temperature + "℃");
                warnInfoList.add(tempWarn);
            }
        }
        
        return warnInfoList;
    }

    /**
     * 预警信息类
     */
    public static class WarnInfo {
        private Long attractionId;
        private String warnType;
        private String warnLevel;
        private String warnMessage;
        private String warnValue;
        private String createTime;

        public Long getAttractionId() {
            return attractionId;
        }

        public void setAttractionId(Long attractionId) {
            this.attractionId = attractionId;
        }

        public String getWarnType() {
            return warnType;
        }

        public void setWarnType(String warnType) {
            this.warnType = warnType;
        }

        public String getWarnLevel() {
            return warnLevel;
        }

        public void setWarnLevel(String warnLevel) {
            this.warnLevel = warnLevel;
        }

        public String getWarnMessage() {
            return warnMessage;
        }

        public void setWarnMessage(String warnMessage) {
            this.warnMessage = warnMessage;
        }

        public String getWarnValue() {
            return warnValue;
        }

        public void setWarnValue(String warnValue) {
            this.warnValue = warnValue;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
    }
}
