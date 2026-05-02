package travel.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import travel.entity.travel_recommendation.Attraction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("route_transport")
public class RouteTransport {

    @TableId(type = IdType.AUTO)
    private Integer routeTransportId;

    @TableField("route_id")
    private Integer routeId;

    @TableField(exist = false)
    private Route route;

    @TableField("from_attraction_id")
    private Integer fromAttractionId;

    @TableField(exist = false)
    private Attraction fromAttraction;

    @TableField("to_attraction_id")
    private Integer toAttractionId;

    @TableField(exist = false)
    private Attraction toAttraction;

    @TableField("transport_id")
    private Integer transportId;

    @TableField(exist = false)
    private Transport transport;

    @TableField("transport_order")
    private Integer transportOrder;

    @TableField("estimated_duration")
    private Integer estimatedDuration;

    @TableField("distance")
    private BigDecimal distance;

    @TableField("instructions")
    private String instructions;

    @TableField("cost_estimate")
    private BigDecimal costEstimate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Integer getRouteTransportId() {
        return routeTransportId;
    }

    public void setRouteTransportId(Integer routeTransportId) {
        this.routeTransportId = routeTransportId;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Integer getFromAttractionId() {
        return fromAttractionId;
    }

    public void setFromAttractionId(Integer fromAttractionId) {
        this.fromAttractionId = fromAttractionId;
    }

    public Attraction getFromAttraction() {
        return fromAttraction;
    }

    public void setFromAttraction(Attraction fromAttraction) {
        this.fromAttraction = fromAttraction;
    }

    public Integer getToAttractionId() {
        return toAttractionId;
    }

    public void setToAttractionId(Integer toAttractionId) {
        this.toAttractionId = toAttractionId;
    }

    public Attraction getToAttraction() {
        return toAttraction;
    }

    public void setToAttraction(Attraction toAttraction) {
        this.toAttraction = toAttraction;
    }

    public Integer getTransportId() {
        return transportId;
    }

    public void setTransportId(Integer transportId) {
        this.transportId = transportId;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Integer getTransportOrder() {
        return transportOrder;
    }

    public void setTransportOrder(Integer transportOrder) {
        this.transportOrder = transportOrder;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public BigDecimal getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(BigDecimal costEstimate) {
        this.costEstimate = costEstimate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getRouteIdAsLong() {
        return routeId != null ? routeId.longValue() : null;
    }

    public Long getFromAttractionIdAsLong() {
        return fromAttractionId != null ? fromAttractionId.longValue() : null;
    }

    public Long getToAttractionIdAsLong() {
        return toAttractionId != null ? toAttractionId.longValue() : null;
    }

    public Long getTransportIdAsLong() {
        return transportId != null ? transportId.longValue() : null;
    }
}