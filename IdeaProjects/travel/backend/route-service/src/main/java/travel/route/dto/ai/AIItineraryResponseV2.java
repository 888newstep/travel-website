package travel.route.dto.ai;

public class AIItineraryResponseV2 {

    private String destination;

    private Integer days;

    private String itinerary;

    private String source;

    public AIItineraryResponseV2() {
    }

    public AIItineraryResponseV2(String destination, Integer days, String itinerary, String source) {
        this.destination = destination;
        this.days = days;
        this.itinerary = itinerary;
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

    public String getItinerary() {
        return itinerary;
    }

    public void setItinerary(String itinerary) {
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
        private String destination;
        private Integer days;
        private String itinerary;
        private String source;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder itinerary(String itinerary) {
            this.itinerary = itinerary;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIItineraryResponseV2 build() {
            return new AIItineraryResponseV2(destination, days, itinerary, source);
        }
    }

}