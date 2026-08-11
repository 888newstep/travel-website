package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class AIAnalyzeImageResponse {

    private Boolean success;

    private String analysisType;

    /**
     * 百度 AI 的结果字段随分析类型变化，使用 JsonNode 保留动态 JSON 结构，避免 Object 向客户端扩散。
     */
    private Map<String, JsonNode> details;

    private String error;

    public AIAnalyzeImageResponse() {
    }

    public AIAnalyzeImageResponse(Boolean success, String analysisType, Map<String, JsonNode> details, String error) {
        this.success = success;
        this.analysisType = analysisType;
        this.details = details;
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

    public Map<String, JsonNode> getDetails() {
        return details;
    }

    public void setDetails(Map<String, JsonNode> details) {
        this.details = details;
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
        private Map<String, JsonNode> details;
        private String error;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder analysisType(String analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public Builder details(Map<String, JsonNode> details) {
            this.details = details;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public AIAnalyzeImageResponse build() {
            return new AIAnalyzeImageResponse(success, analysisType, details, error);
        }
    }

}
