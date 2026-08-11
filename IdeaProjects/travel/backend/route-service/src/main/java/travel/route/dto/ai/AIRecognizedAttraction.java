package travel.route.dto.ai;

public class AIRecognizedAttraction {

    private String name;

    private Double confidence;

    private String location;

    private String description;

    private Double rating;

    public AIRecognizedAttraction() {
    }

    public AIRecognizedAttraction(String name, Double confidence, String location, String description, Double rating) {
        this.name = name;
        this.confidence = confidence;
        this.location = location;
        this.description = description;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private Double confidence;
        private String location;
        private String description;
        private Double rating;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public AIRecognizedAttraction build() {
            return new AIRecognizedAttraction(name, confidence, location, description, rating);
        }
    }

}