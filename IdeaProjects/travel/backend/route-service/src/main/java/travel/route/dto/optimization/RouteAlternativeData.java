package travel.route.dto.optimization;

/**
 * Stable metrics returned for one alternative route.
 * Keep the property name routeData on the parent DTO for JSON compatibility.
 */
public class RouteAlternativeData {

    private String preference;
    private Double fitness;
    private Double totalDistance;
    private Double totalCost;
    private Double totalTime;

    public RouteAlternativeData() {
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Double getFitness() {
        return fitness;
    }

    public void setFitness(Double fitness) {
        this.fitness = fitness;
    }

    public Double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public Double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Double totalTime) {
        this.totalTime = totalTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RouteAlternativeData data = new RouteAlternativeData();

        public Builder preference(String preference) {
            data.preference = preference;
            return this;
        }

        public Builder fitness(Double fitness) {
            data.fitness = fitness;
            return this;
        }

        public Builder totalDistance(Double totalDistance) {
            data.totalDistance = totalDistance;
            return this;
        }

        public Builder totalCost(Double totalCost) {
            data.totalCost = totalCost;
            return this;
        }

        public Builder totalTime(Double totalTime) {
            data.totalTime = totalTime;
            return this;
        }

        public RouteAlternativeData build() {
            return data;
        }
    }
}
