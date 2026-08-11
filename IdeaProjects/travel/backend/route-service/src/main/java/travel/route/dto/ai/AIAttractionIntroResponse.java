package travel.route.dto.ai;

public class AIAttractionIntroResponse {

    private Boolean success;

    private Integer attractionId;

    private String name;

    private String briefIntro;

    private String detailedIntro;

    private String funFacts;

    private String bestVisitTime;

    private String estimatedDuration;

    private String source;

    private String message;

    public AIAttractionIntroResponse() {
    }

    public AIAttractionIntroResponse(Boolean success, Integer attractionId, String name, String briefIntro, String detailedIntro, String funFacts, String bestVisitTime, String estimatedDuration, String source, String message) {
        this.success = success;
        this.attractionId = attractionId;
        this.name = name;
        this.briefIntro = briefIntro;
        this.detailedIntro = detailedIntro;
        this.funFacts = funFacts;
        this.bestVisitTime = bestVisitTime;
        this.estimatedDuration = estimatedDuration;
        this.source = source;
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getAttractionId() {
        return attractionId;
    }

    public void setAttractionId(Integer attractionId) {
        this.attractionId = attractionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBriefIntro() {
        return briefIntro;
    }

    public void setBriefIntro(String briefIntro) {
        this.briefIntro = briefIntro;
    }

    public String getDetailedIntro() {
        return detailedIntro;
    }

    public void setDetailedIntro(String detailedIntro) {
        this.detailedIntro = detailedIntro;
    }

    public String getFunFacts() {
        return funFacts;
    }

    public void setFunFacts(String funFacts) {
        this.funFacts = funFacts;
    }

    public String getBestVisitTime() {
        return bestVisitTime;
    }

    public void setBestVisitTime(String bestVisitTime) {
        this.bestVisitTime = bestVisitTime;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private Integer attractionId;
        private String name;
        private String briefIntro;
        private String detailedIntro;
        private String funFacts;
        private String bestVisitTime;
        private String estimatedDuration;
        private String source;
        private String message;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder attractionId(Integer attractionId) {
            this.attractionId = attractionId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder briefIntro(String briefIntro) {
            this.briefIntro = briefIntro;
            return this;
        }

        public Builder detailedIntro(String detailedIntro) {
            this.detailedIntro = detailedIntro;
            return this;
        }

        public Builder funFacts(String funFacts) {
            this.funFacts = funFacts;
            return this;
        }

        public Builder bestVisitTime(String bestVisitTime) {
            this.bestVisitTime = bestVisitTime;
            return this;
        }

        public Builder estimatedDuration(String estimatedDuration) {
            this.estimatedDuration = estimatedDuration;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public AIAttractionIntroResponse build() {
            return new AIAttractionIntroResponse(success, attractionId, name, briefIntro, detailedIntro, funFacts, bestVisitTime, estimatedDuration, source, message);
        }
    }

}