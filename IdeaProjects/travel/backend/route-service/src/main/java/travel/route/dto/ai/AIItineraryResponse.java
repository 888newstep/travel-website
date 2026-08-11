package travel.route.dto.ai;

import java.util.List;

public class AIItineraryResponse {

    private String destination;

    private Integer days;

    private List<AIDailyItinerary> dailyPlan;

    private String summary;

    private String source;

    public AIItineraryResponse() {
    }

    public AIItineraryResponse(String destination, Integer days, List<AIDailyItinerary> dailyPlan, String summary, String source) {
        this.destination = destination;
        this.days = days;
        this.dailyPlan = dailyPlan;
        this.summary = summary;
        this.source = source;
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

    public List<AIDailyItinerary> getDailyPlan() {
        return dailyPlan;
    }

    public void setDailyPlan(List<AIDailyItinerary> dailyPlan) {
        this.dailyPlan = dailyPlan;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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
        private String destination;
        private Integer days;
        private List<AIDailyItinerary> dailyPlan;
        private String summary;
        private String source;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder dailyPlan(List<AIDailyItinerary> dailyPlan) {
            this.dailyPlan = dailyPlan;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIItineraryResponse build() {
            return new AIItineraryResponse(destination, days, dailyPlan, summary, source);
        }
    }

}
