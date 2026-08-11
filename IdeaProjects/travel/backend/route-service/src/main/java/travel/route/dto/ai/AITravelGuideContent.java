package travel.route.dto.ai;

import java.time.LocalDateTime;

/**
 * 旅游攻略生成结果。
 *
 * <p>字段名称与历史 Map 返回结构保持一致，避免客户端升级时发生 JSON 兼容性变化。</p>
 */
public class AITravelGuideContent {

    private Boolean success;
    private Integer cityId;
    private String cityName;
    private Integer days;
    private LocalDateTime generatedAt;
    private AITravelGuideSection guideContent;
    private Integer guideQualityScore;

    public AITravelGuideContent() {
    }

    public AITravelGuideContent(Boolean success, Integer cityId, String cityName, Integer days,
                                LocalDateTime generatedAt, AITravelGuideSection guideContent,
                                Integer guideQualityScore) {
        this.success = success;
        this.cityId = cityId;
        this.cityName = cityName;
        this.days = days;
        this.generatedAt = generatedAt;
        this.guideContent = guideContent;
        this.guideQualityScore = guideQualityScore;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public AITravelGuideSection getGuideContent() {
        return guideContent;
    }

    public void setGuideContent(AITravelGuideSection guideContent) {
        this.guideContent = guideContent;
    }

    public Integer getGuideQualityScore() {
        return guideQualityScore;
    }

    public void setGuideQualityScore(Integer guideQualityScore) {
        this.guideQualityScore = guideQualityScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private Integer cityId;
        private String cityName;
        private Integer days;
        private LocalDateTime generatedAt;
        private AITravelGuideSection guideContent;
        private Integer guideQualityScore;

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder cityName(String cityName) {
            this.cityName = cityName;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder generatedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder guideContent(AITravelGuideSection guideContent) {
            this.guideContent = guideContent;
            return this;
        }

        public Builder guideQualityScore(Integer guideQualityScore) {
            this.guideQualityScore = guideQualityScore;
            return this;
        }

        public AITravelGuideContent build() {
            return new AITravelGuideContent(success, cityId, cityName, days, generatedAt,
                    guideContent, guideQualityScore);
        }
    }
}
