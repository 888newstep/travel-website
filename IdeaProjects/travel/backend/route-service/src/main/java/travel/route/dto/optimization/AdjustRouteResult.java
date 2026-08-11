package travel.route.dto.optimization;

import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import java.util.List;
import java.util.Map;

public class AdjustRouteResult {
    private Route originalRoute;
    private Attraction addedAttraction;
    private Integer removedAttractionId;
    private List<Integer> newOrder;
    private Integer newDays;
    private Boolean success;

    public AdjustRouteResult() {}

    public Route getOriginalRoute() { return originalRoute; }
    public void setOriginalRoute(Route originalRoute) { this.originalRoute = originalRoute; }
    public Attraction getAddedAttraction() { return addedAttraction; }
    public void setAddedAttraction(Attraction addedAttraction) { this.addedAttraction = addedAttraction; }
    public Integer getRemovedAttractionId() { return removedAttractionId; }
    public void setRemovedAttractionId(Integer removedAttractionId) { this.removedAttractionId = removedAttractionId; }
    public List<Integer> getNewOrder() { return newOrder; }
    public void setNewOrder(List<Integer> newOrder) { this.newOrder = newOrder; }
    public Integer getNewDays() { return newDays; }
    public void setNewDays(Integer newDays) { this.newDays = newDays; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final AdjustRouteResult r = new AdjustRouteResult();
        public Builder originalRoute(Route originalRoute) { r.originalRoute = originalRoute; return this; }
        public Builder addedAttraction(Attraction addedAttraction) { r.addedAttraction = addedAttraction; return this; }
        public Builder removedAttractionId(Integer removedAttractionId) { r.removedAttractionId = removedAttractionId; return this; }
        public Builder newOrder(List<Integer> newOrder) { r.newOrder = newOrder; return this; }
        public Builder newDays(Integer newDays) { r.newDays = newDays; return this; }
        public Builder success(Boolean success) { r.success = success; return this; }
        public AdjustRouteResult build() { return r; }
    }
}
