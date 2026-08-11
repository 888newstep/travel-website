package travel.route.dto.optimization;

/**
 * Typed crowd prediction for one attraction in a route.
 */
public class RouteCrowdPredictionItem {

    private Integer attractionId;
    private String attractionName;
    private Integer predictedCrowd;
    private String crowdLevel;
    private String suggestedTime;

    public RouteCrowdPredictionItem() {
    }

    public Integer getAttractionId() {
        return attractionId;
    }

    public void setAttractionId(Integer attractionId) {
        this.attractionId = attractionId;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public Integer getPredictedCrowd() {
        return predictedCrowd;
    }

    public void setPredictedCrowd(Integer predictedCrowd) {
        this.predictedCrowd = predictedCrowd;
    }

    public String getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(String crowdLevel) {
        this.crowdLevel = crowdLevel;
    }

    public String getSuggestedTime() {
        return suggestedTime;
    }

    public void setSuggestedTime(String suggestedTime) {
        this.suggestedTime = suggestedTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RouteCrowdPredictionItem item = new RouteCrowdPredictionItem();

        public Builder attractionId(Integer attractionId) {
            item.attractionId = attractionId;
            return this;
        }

        public Builder attractionName(String attractionName) {
            item.attractionName = attractionName;
            return this;
        }

        public Builder predictedCrowd(Integer predictedCrowd) {
            item.predictedCrowd = predictedCrowd;
            return this;
        }

        public Builder crowdLevel(String crowdLevel) {
            item.crowdLevel = crowdLevel;
            return this;
        }

        public Builder suggestedTime(String suggestedTime) {
            item.suggestedTime = suggestedTime;
            return this;
        }

        public RouteCrowdPredictionItem build() {
            return item;
        }
    }
}
