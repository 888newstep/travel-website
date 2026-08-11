package travel.route.dto.optimization;

import java.util.List;

/**
 * Stable daily plan fields exposed by route recommendations.
 * The richer algorithm plan remains available through the route field.
 */
public class RouteRecommendationDayPlan {

    private Integer dayNumber;
    private List<Integer> attractionIds;
    private Double distance;
    private Double cost;
    private Double time;

    public RouteRecommendationDayPlan() {
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public List<Integer> getAttractionIds() {
        return attractionIds;
    }

    public void setAttractionIds(List<Integer> attractionIds) {
        this.attractionIds = attractionIds;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RouteRecommendationDayPlan plan = new RouteRecommendationDayPlan();

        public Builder dayNumber(Integer dayNumber) {
            plan.dayNumber = dayNumber;
            return this;
        }

        public Builder attractionIds(List<Integer> attractionIds) {
            plan.attractionIds = attractionIds;
            return this;
        }

        public Builder distance(Double distance) {
            plan.distance = distance;
            return this;
        }

        public Builder cost(Double cost) {
            plan.cost = cost;
            return this;
        }

        public Builder time(Double time) {
            plan.time = time;
            return this;
        }

        public RouteRecommendationDayPlan build() {
            return plan;
        }
    }
}
