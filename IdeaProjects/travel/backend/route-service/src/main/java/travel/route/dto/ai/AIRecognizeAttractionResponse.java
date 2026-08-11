package travel.route.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

public class AIRecognizeAttractionResponse {

    private Boolean success;

    private String type;

    private LocalDateTime timestamp;

    private List<AIRecognizedAttraction> attractions;

    private AIRecognizedAttraction topMatch;

    private String error;

    public AIRecognizeAttractionResponse() {
    }

    public AIRecognizeAttractionResponse(Boolean success, String type, LocalDateTime timestamp, List<AIRecognizedAttraction> attractions, AIRecognizedAttraction topMatch, String error) {
        this.success = success;
        this.type = type;
        this.timestamp = timestamp;
        this.attractions = attractions;
        this.topMatch = topMatch;
        this.error = error;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<AIRecognizedAttraction> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<AIRecognizedAttraction> attractions) {
        this.attractions = attractions;
    }

    public AIRecognizedAttraction getTopMatch() {
        return topMatch;
    }

    public void setTopMatch(AIRecognizedAttraction topMatch) {
        this.topMatch = topMatch;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private String type;
        private LocalDateTime timestamp;
        private List<AIRecognizedAttraction> attractions;
        private AIRecognizedAttraction topMatch;
        private String error;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder attractions(List<AIRecognizedAttraction> attractions) {
            this.attractions = attractions;
            return this;
        }

        public Builder topMatch(AIRecognizedAttraction topMatch) {
            this.topMatch = topMatch;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public AIRecognizeAttractionResponse build() {
            return new AIRecognizeAttractionResponse(success, type, timestamp, attractions, topMatch, error);
        }
    }

}