package travel.route.dto.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AISafetyAdviceResponse {

    private Boolean success;

    private Integer cityId;

    private String cityName;

    private LocalDateTime advisedAt;

    private String safetyLevel;

    private Integer safetyScore;

    private List<String> generalAdvice;

    private List<String> travelAdvice;

    private Map<String, List<String>> areaAdvice;

    public AISafetyAdviceResponse() {
    }

    public AISafetyAdviceResponse(Boolean success, Integer cityId, String cityName, LocalDateTime advisedAt, String safetyLevel, Integer safetyScore, List<String> generalAdvice, List<String> travelAdvice, Map<String, List<String>> areaAdvice) {
        this.success = success;
        this.cityId = cityId;
        this.cityName = cityName;
        this.advisedAt = advisedAt;
        this.safetyLevel = safetyLevel;
        this.safetyScore = safetyScore;
        this.generalAdvice = generalAdvice;
        this.travelAdvice = travelAdvice;
        this.areaAdvice = areaAdvice;
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

    public LocalDateTime getAdvisedAt() {
        return advisedAt;
    }

    public void setAdvisedAt(LocalDateTime advisedAt) {
        this.advisedAt = advisedAt;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(String safetyLevel) {
        this.safetyLevel = safetyLevel;
    }

    public Integer getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Integer safetyScore) {
        this.safetyScore = safetyScore;
    }

    public List<String> getGeneralAdvice() {
        return generalAdvice;
    }

    public void setGeneralAdvice(List<String> generalAdvice) {
        this.generalAdvice = generalAdvice;
    }

    public List<String> getTravelAdvice() {
        return travelAdvice;
    }

    public void setTravelAdvice(List<String> travelAdvice) {
        this.travelAdvice = travelAdvice;
    }

    public Map<String, List<String>> getAreaAdvice() {
        return areaAdvice;
    }

    public void setAreaAdvice(Map<String, List<String>> areaAdvice) {
        this.areaAdvice = areaAdvice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private Integer cityId;
        private String cityName;
        private LocalDateTime advisedAt;
        private String safetyLevel;
        private Integer safetyScore;
        private List<String> generalAdvice;
        private List<String> travelAdvice;
        private Map<String, List<String>> areaAdvice;

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

        public Builder advisedAt(LocalDateTime advisedAt) {
            this.advisedAt = advisedAt;
            return this;
        }

        public Builder safetyLevel(String safetyLevel) {
            this.safetyLevel = safetyLevel;
            return this;
        }

        public Builder safetyScore(Integer safetyScore) {
            this.safetyScore = safetyScore;
            return this;
        }

        public Builder generalAdvice(List<String> generalAdvice) {
            this.generalAdvice = generalAdvice;
            return this;
        }

        public Builder travelAdvice(List<String> travelAdvice) {
            this.travelAdvice = travelAdvice;
            return this;
        }

        public Builder areaAdvice(Map<String, List<String>> areaAdvice) {
            this.areaAdvice = areaAdvice;
            return this;
        }

        public AISafetyAdviceResponse build() {
            return new AISafetyAdviceResponse(success, cityId, cityName, advisedAt, safetyLevel, safetyScore, generalAdvice, travelAdvice, areaAdvice);
        }
    }

}