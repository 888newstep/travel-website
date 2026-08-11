package travel.route.dto.ai;

import java.util.List;

public class AIOptimizeRouteResponse {

    private Boolean success;

    private Integer routeId;

    private List<AIOptimizeSuggestion> suggestions;

    private Integer optimizedScore;

    private String source;

    private String message;

    public AIOptimizeRouteResponse() {
    }

    public AIOptimizeRouteResponse(Boolean success, Integer routeId, List<AIOptimizeSuggestion> suggestions, Integer optimizedScore, String source, String message) {
        this.success = success;
        this.routeId = routeId;
        this.suggestions = suggestions;
        this.optimizedScore = optimizedScore;
        this.source = source;
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public List<AIOptimizeSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<AIOptimizeSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public Integer getOptimizedScore() {
        return optimizedScore;
    }

    public void setOptimizedScore(Integer optimizedScore) {
        this.optimizedScore = optimizedScore;
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
        private Integer routeId;
        private List<AIOptimizeSuggestion> suggestions;
        private Integer optimizedScore;
        private String source;
        private String message;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder routeId(Integer routeId) {
            this.routeId = routeId;
            return this;
        }

        public Builder suggestions(List<AIOptimizeSuggestion> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public Builder optimizedScore(Integer optimizedScore) {
            this.optimizedScore = optimizedScore;
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

        public AIOptimizeRouteResponse build() {
            return new AIOptimizeRouteResponse(success, routeId, suggestions, optimizedScore, source, message);
        }
    }

}