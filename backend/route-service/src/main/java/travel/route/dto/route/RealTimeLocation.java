package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * 实时调整请求中的当前位置。
 *
 * <p>保留历史 JSON 字段 lat/lng，同时兼容 latitude/longitude 输入。</p>
 */
public class RealTimeLocation {

    @JsonAlias("latitude")
    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private Double lat;

    @JsonAlias("longitude")
    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private Double lng;

    public RealTimeLocation() {
    }

    public RealTimeLocation(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    @JsonIgnore
    @AssertTrue(message = "当前位置的纬度和经度必须同时提供")
    public boolean isCoordinatePairComplete() {
        return (lat == null && lng == null) || (lat != null && lng != null);
    }
}
