package travel.route.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预算估算服务的结构化结果。
 */
public class AIBudgetDetails {

    private Boolean success;
    private Integer cityId;
    private String cityName;
    private Integer days;
    private LocalDateTime estimatedAt;
    private AIBudgetBreakdown budgetDetails;
    private List<String> savingTips;
    private String currency;

    public AIBudgetDetails() {
    }

    public AIBudgetDetails(Boolean success, Integer cityId, String cityName, Integer days,
                           LocalDateTime estimatedAt, AIBudgetBreakdown budgetDetails,
                           List<String> savingTips, String currency) {
        this.success = success;
        this.cityId = cityId;
        this.cityName = cityName;
        this.days = days;
        this.estimatedAt = estimatedAt;
        this.budgetDetails = budgetDetails;
        this.savingTips = savingTips;
        this.currency = currency;
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

    public LocalDateTime getEstimatedAt() {
        return estimatedAt;
    }

    public void setEstimatedAt(LocalDateTime estimatedAt) {
        this.estimatedAt = estimatedAt;
    }

    public AIBudgetBreakdown getBudgetDetails() {
        return budgetDetails;
    }

    public void setBudgetDetails(AIBudgetBreakdown budgetDetails) {
        this.budgetDetails = budgetDetails;
    }

    public List<String> getSavingTips() {
        return savingTips;
    }

    public void setSavingTips(List<String> savingTips) {
        this.savingTips = savingTips;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean success;
        private Integer cityId;
        private String cityName;
        private Integer days;
        private LocalDateTime estimatedAt;
        private AIBudgetBreakdown budgetDetails;
        private List<String> savingTips;
        private String currency;

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

        public Builder estimatedAt(LocalDateTime estimatedAt) {
            this.estimatedAt = estimatedAt;
            return this;
        }

        public Builder budgetDetails(AIBudgetBreakdown budgetDetails) {
            this.budgetDetails = budgetDetails;
            return this;
        }

        public Builder savingTips(List<String> savingTips) {
            this.savingTips = savingTips;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public AIBudgetDetails build() {
            return new AIBudgetDetails(success, cityId, cityName, days, estimatedAt,
                    budgetDetails, savingTips, currency);
        }
    }
}
