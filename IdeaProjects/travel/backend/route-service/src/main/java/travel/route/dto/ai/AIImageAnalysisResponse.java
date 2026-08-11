package travel.route.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

public class AIImageAnalysisResponse {

    private Boolean success;

    private String analysisType;

    private LocalDateTime timestamp;

    private AIImageContentAnalysis contentAnalysis;

    private AIImageQualityAnalysis qualityAnalysis;

    private List<AIImageRecommendation> recommendations;

    private Double confidence;

    private String error;

    public AIImageAnalysisResponse() {
    }

    public AIImageAnalysisResponse(Boolean success, String analysisType, LocalDateTime timestamp, AIImageContentAnalysis contentAnalysis, AIImageQualityAnalysis qualityAnalysis, List<AIImageRecommendation> recommendations, Double confidence, String error) {
        this.success = success;
        this.analysisType = analysisType;
        this.timestamp = timestamp;
        this.contentAnalysis = contentAnalysis;
        this.qualityAnalysis = qualityAnalysis;
        this.recommendations = recommendations;
        this.confidence = confidence;
        this.error = error;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AIImageContentAnalysis getContentAnalysis() {
        return contentAnalysis;
    }

    public void setContentAnalysis(AIImageContentAnalysis contentAnalysis) {
        this.contentAnalysis = contentAnalysis;
    }

    public AIImageQualityAnalysis getQualityAnalysis() {
        return qualityAnalysis;
    }

    public void setQualityAnalysis(AIImageQualityAnalysis qualityAnalysis) {
        this.qualityAnalysis = qualityAnalysis;
    }

    public List<AIImageRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AIImageRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
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
        private String analysisType;
        private LocalDateTime timestamp;
        private AIImageContentAnalysis contentAnalysis;
        private AIImageQualityAnalysis qualityAnalysis;
        private List<AIImageRecommendation> recommendations;
        private Double confidence;
        private String error;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder analysisType(String analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder contentAnalysis(AIImageContentAnalysis contentAnalysis) {
            this.contentAnalysis = contentAnalysis;
            return this;
        }

        public Builder qualityAnalysis(AIImageQualityAnalysis qualityAnalysis) {
            this.qualityAnalysis = qualityAnalysis;
            return this;
        }

        public Builder recommendations(List<AIImageRecommendation> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public AIImageAnalysisResponse build() {
            return new AIImageAnalysisResponse(success, analysisType, timestamp, contentAnalysis, qualityAnalysis, recommendations, confidence, error);
        }
    }

}