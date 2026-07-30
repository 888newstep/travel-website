package travel.common.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("transport")
public class Transport {

    @TableId(type = IdType.AUTO)
    private Integer transportId;

    @TableField("transport_name")
    private String transportName;

    @TableField("transport_type")
    private TransportType transportType;

    @TableField("icon_url")
    private String iconUrl;

    @TableField("avg_speed_kmh")
    private BigDecimal avgSpeedKmh;

    @TableField("cost_per_km")
    private BigDecimal costPerKm;

    @TableField("co2_emission")
    private BigDecimal co2Emission;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Integer getTransportId() {
        return transportId;
    }

    public void setTransportId(Integer transportId) {
        this.transportId = transportId;
    }

    public String getTransportName() {
        return transportName;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public BigDecimal getAvgSpeedKmh() {
        return avgSpeedKmh;
    }

    public void setAvgSpeedKmh(BigDecimal avgSpeedKmh) {
        this.avgSpeedKmh = avgSpeedKmh;
    }

    public BigDecimal getCostPerKm() {
        return costPerKm;
    }

    public void setCostPerKm(BigDecimal costPerKm) {
        this.costPerKm = costPerKm;
    }

    public BigDecimal getCo2Emission() {
        return co2Emission;
    }

    public void setCo2Emission(BigDecimal co2Emission) {
        this.co2Emission = co2Emission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Additional getters for controller compatibility
    public TransportType getType() {
        return getTransportType();
    }

    public String getName() {
        return getTransportName();
    }
}
