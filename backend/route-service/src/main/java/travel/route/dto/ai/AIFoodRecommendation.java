package travel.route.dto.ai;

import java.util.List;

/**
 * 旅游攻略中的美食推荐。
 */
public class AIFoodRecommendation {

    private String name;
    private String description;
    private List<String> recommendedRestaurants;

    public AIFoodRecommendation() {
    }

    public AIFoodRecommendation(String name, String description, List<String> recommendedRestaurants) {
        this.name = name;
        this.description = description;
        this.recommendedRestaurants = recommendedRestaurants;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRecommendedRestaurants() {
        return recommendedRestaurants;
    }

    public void setRecommendedRestaurants(List<String> recommendedRestaurants) {
        this.recommendedRestaurants = recommendedRestaurants;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private List<String> recommendedRestaurants;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder recommendedRestaurants(List<String> recommendedRestaurants) {
            this.recommendedRestaurants = recommendedRestaurants;
            return this;
        }

        public AIFoodRecommendation build() {
            return new AIFoodRecommendation(name, description, recommendedRestaurants);
        }
    }
}
