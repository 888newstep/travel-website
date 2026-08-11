package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AIAnalyzeImageRequest {

        @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String analysisType;

    public AIAnalyzeImageRequest() {
    }

    public AIAnalyzeImageRequest(String imageUrl, String analysisType) {
        this.imageUrl = imageUrl;
        this.analysisType = analysisType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String imageUrl;
        private String analysisType;

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder analysisType(String analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public AIAnalyzeImageRequest build() {
            return new AIAnalyzeImageRequest(imageUrl, analysisType);
        }
    }

}