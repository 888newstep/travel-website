package travel.route.dto.ai;


public class AISmartItineraryOptimizeResponse {

    private Integer routeId;

    private AISmartItineraryOptimization optimized;

    private String source;

    public AISmartItineraryOptimizeResponse() {
    }

    public AISmartItineraryOptimizeResponse(Integer routeId, AISmartItineraryOptimization optimized, String source) {
        this.routeId = routeId;
        this.optimized = optimized;
        this.source = source;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public AISmartItineraryOptimization getOptimized() {
        return optimized;
    }

    public void setOptimized(AISmartItineraryOptimization optimized) {
        this.optimized = optimized;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer routeId;
        private AISmartItineraryOptimization optimized;
        private String source;

        public Builder routeId(Integer routeId) {
            this.routeId = routeId;
            return this;
        }

        public Builder optimized(AISmartItineraryOptimization optimized) {
            this.optimized = optimized;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AISmartItineraryOptimizeResponse build() {
            return new AISmartItineraryOptimizeResponse(routeId, optimized, source);
        }
    }

}
