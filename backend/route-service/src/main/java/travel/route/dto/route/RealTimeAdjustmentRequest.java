package travel.route.dto.route;

import jakarta.validation.Valid;

public class RealTimeAdjustmentRequest {
    @Valid
    private RealTimeLocation currentLocation;

    @Valid
    private RealTimeFactors realTimeFactors;

    public RealTimeAdjustmentRequest() {}

    public RealTimeLocation getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(RealTimeLocation currentLocation) { this.currentLocation = currentLocation; }
    public RealTimeFactors getRealTimeFactors() { return realTimeFactors; }
    public void setRealTimeFactors(RealTimeFactors realTimeFactors) { this.realTimeFactors = realTimeFactors; }
}
