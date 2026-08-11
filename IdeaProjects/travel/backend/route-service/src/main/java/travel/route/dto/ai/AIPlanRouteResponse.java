package travel.route.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

public class AIPlanRouteResponse {

    private Boolean success;

    private String planType;

    private LocalDateTime timestamp;

    private String destination;

    private Integer days;

    private String travelStyle;

    private List<AIDailyPlan> dailyPlans;

    private Integer estimatedCost;

    private Integer optimizationScore;

    public AIPlanRouteResponse() {
    }

    public AIPlanRouteResponse(Boolean success, String planType, LocalDateTime timestamp, String destination, Integer days, String travelStyle, List<AIDailyPlan> dailyPlans, Integer estimatedCost, Integer optimizationScore) {
        this.success = success;
        this.planType = planType;
        this.timestamp = timestamp;
        this.destination = destination;
        this.days = days;
        this.travelStyle = travelStyle;
        this.dailyPlans = dailyPlans;
        this.estimatedCost = estimatedCost;
        this.optimizationScore = optimizationScore;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public List<AIDailyPlan> getDailyPlans() {
        return dailyPlans;
    }

    public void setDailyPlans(List<AIDailyPlan> dailyPlans) {
        this.dailyPlans = dailyPlans;
    }

    public Integer getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Integer estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Integer getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(Integer optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private String planType;
        private LocalDateTime timestamp;
        private String destination;
        private Integer days;
        private String travelStyle;
        private List<AIDailyPlan> dailyPlans;
        private Integer estimatedCost;
        private Integer optimizationScore;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder planType(String planType) {
            this.planType = planType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder travelStyle(String travelStyle) {
            this.travelStyle = travelStyle;
            return this;
        }

        public Builder dailyPlans(List<AIDailyPlan> dailyPlans) {
            this.dailyPlans = dailyPlans;
            return this;
        }

        public Builder estimatedCost(Integer estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        public Builder optimizationScore(Integer optimizationScore) {
            this.optimizationScore = optimizationScore;
            return this;
        }

        public AIPlanRouteResponse build() {
            return new AIPlanRouteResponse(success, planType, timestamp, destination, days, travelStyle, dailyPlans, estimatedCost, optimizationScore);
        }
    }

}