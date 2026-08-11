package travel.route.dto.ai;

public class AITravelGuideResponse {

    private Integer cityId;

    private Integer days;

    private AITravelGuideContent guide;

    private String source;

    public AITravelGuideResponse() {
    }

    public AITravelGuideResponse(Integer cityId, Integer days, AITravelGuideContent guide, String source) {
        this.cityId = cityId;
        this.days = days;
        this.guide = guide;
        this.source = source;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public AITravelGuideContent getGuide() {
        return guide;
    }

    public void setGuide(AITravelGuideContent guide) {
        this.guide = guide;
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
        private Integer cityId;
        private Integer days;
        private AITravelGuideContent guide;
        private String source;

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder guide(AITravelGuideContent guide) {
            this.guide = guide;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AITravelGuideResponse build() {
            return new AITravelGuideResponse(cityId, days, guide, source);
        }
    }

}
