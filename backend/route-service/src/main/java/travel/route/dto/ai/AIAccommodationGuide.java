package travel.route.dto.ai;

import java.util.List;

/**
 * 旅游攻略中的住宿建议。
 */
public class AIAccommodationGuide {

    private String budget;
    private List<String> recommendedAreas;
    private String tips;

    public AIAccommodationGuide() {
    }

    public AIAccommodationGuide(String budget, List<String> recommendedAreas, String tips) {
        this.budget = budget;
        this.recommendedAreas = recommendedAreas;
        this.tips = tips;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public List<String> getRecommendedAreas() {
        return recommendedAreas;
    }

    public void setRecommendedAreas(List<String> recommendedAreas) {
        this.recommendedAreas = recommendedAreas;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String budget;
        private List<String> recommendedAreas;
        private String tips;

        public Builder budget(String budget) {
            this.budget = budget;
            return this;
        }

        public Builder recommendedAreas(List<String> recommendedAreas) {
            this.recommendedAreas = recommendedAreas;
            return this;
        }

        public Builder tips(String tips) {
            this.tips = tips;
            return this;
        }

        public AIAccommodationGuide build() {
            return new AIAccommodationGuide(budget, recommendedAreas, tips);
        }
    }
}
