package travel.route.dto.optimization;

import java.util.List;

public class RouteCrowdPrediction {
    private Integer routeId;
    private String routeName;
    private String predictDate;
    private Boolean isWeekend;
    private Boolean isHoliday;
    private List<RouteCrowdPredictionItem> crowdPredictions;

    public RouteCrowdPrediction() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getPredictDate() { return predictDate; }
    public void setPredictDate(String predictDate) { this.predictDate = predictDate; }
    public Boolean getIsWeekend() { return isWeekend; }
    public void setIsWeekend(Boolean isWeekend) { this.isWeekend = isWeekend; }
    public Boolean getIsHoliday() { return isHoliday; }
    public void setIsHoliday(Boolean isHoliday) { this.isHoliday = isHoliday; }
    public List<RouteCrowdPredictionItem> getCrowdPredictions() { return crowdPredictions; }
    public void setCrowdPredictions(List<RouteCrowdPredictionItem> crowdPredictions) { this.crowdPredictions = crowdPredictions; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RouteCrowdPrediction p = new RouteCrowdPrediction();
        public Builder routeId(Integer routeId) { p.routeId = routeId; return this; }
        public Builder routeName(String routeName) { p.routeName = routeName; return this; }
        public Builder predictDate(String predictDate) { p.predictDate = predictDate; return this; }
        public Builder isWeekend(Boolean isWeekend) { p.isWeekend = isWeekend; return this; }
        public Builder isHoliday(Boolean isHoliday) { p.isHoliday = isHoliday; return this; }
        public Builder crowdPredictions(List<RouteCrowdPredictionItem> crowdPredictions) { p.crowdPredictions = crowdPredictions; return this; }
        public RouteCrowdPrediction build() { return p; }
    }
}
