package travel.route.dto.ai;


public class AISmartItineraryResponse {

    private Integer userId;

    private Integer cityId;

    private Integer days;

    private Double budget;

    private AISmartItineraryPlan itinerary;

    private String source;

    public AISmartItineraryResponse() {
    }

    public AISmartItineraryResponse(Integer userId, Integer cityId, Integer days, Double budget, AISmartItineraryPlan itinerary, String source) {
        this.userId = userId;
        this.cityId = cityId;
        this.days = days;
        this.budget = budget;
        this.itinerary = itinerary;
        this.source = source;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public AISmartItineraryPlan getItinerary() {
        return itinerary;
    }

    public void setItinerary(AISmartItineraryPlan itinerary) {
        this.itinerary = itinerary;
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
        private Integer userId;
        private Integer cityId;
        private Integer days;
        private Double budget;
        private AISmartItineraryPlan itinerary;
        private String source;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder budget(Double budget) {
            this.budget = budget;
            return this;
        }

        public Builder itinerary(AISmartItineraryPlan itinerary) {
            this.itinerary = itinerary;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AISmartItineraryResponse build() {
            return new AISmartItineraryResponse(userId, cityId, days, budget, itinerary, source);
        }
    }

}
