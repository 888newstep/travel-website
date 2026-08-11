package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实时调整因素。
 *
 * <p>天气、交通和客流是当前稳定业务字段；未来供应商新增字段进入 extensions。</p>
 */
public class RealTimeFactors {

    @Size(max = 32, message = "天气描述不能超过32个字符")
    private String weather;

    @Valid
    private RealTimeTrafficFactors traffic;

    @Valid
    private RealTimeCrowdFactors crowd;

    @Size(max = 20, message = "实时因素扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public RealTimeFactors() {
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public RealTimeTrafficFactors getTraffic() {
        return traffic;
    }

    public void setTraffic(RealTimeTrafficFactors traffic) {
        this.traffic = traffic;
    }

    public RealTimeCrowdFactors getCrowd() {
        return crowd;
    }

    public void setCrowd(RealTimeCrowdFactors crowd) {
        this.crowd = crowd;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= 20) {
            throw new IllegalArgumentException("实时因素扩展字段不能超过20个");
        }
        extensions.put(name, value);
    }

    private void mergeExtensions(Map<String, JsonNode> additionalExtensions) {
        if (additionalExtensions == null || additionalExtensions.isEmpty()) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : additionalExtensions.entrySet()) {
            addExtension(entry.getKey(), entry.getValue());
        }
    }
}
