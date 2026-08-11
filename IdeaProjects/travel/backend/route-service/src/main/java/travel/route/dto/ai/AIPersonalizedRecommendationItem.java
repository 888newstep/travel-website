package travel.route.dto.ai;

import java.time.LocalDateTime;

public class AIPersonalizedRecommendationItem {

    private Integer id;

    private String type;

    private String name;

    private String description;

    private Double rating;

    private Double distance;

    private String priceLevel;

    private Integer days;

    private String difficulty;

    private Double score;

    private LocalDateTime recommendedAt;

    public AIPersonalizedRecommendationItem() {
    }

    public AIPersonalizedRecommendationItem(Integer id, String type, String name, String description, Double rating, Double distance, String priceLevel, Integer days, String difficulty, Double score, LocalDateTime recommendedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.rating = rating;
        this.distance = distance;
        this.priceLevel = priceLevel;
        this.days = days;
        this.difficulty = difficulty;
        this.score = score;
        this.recommendedAt = recommendedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(String priceLevel) {
        this.priceLevel = priceLevel;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalDateTime getRecommendedAt() {
        return recommendedAt;
    }

    public void setRecommendedAt(LocalDateTime recommendedAt) {
        this.recommendedAt = recommendedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String type;
        private String name;
        private String description;
        private Double rating;
        private Double distance;
        private String priceLevel;
        private Integer days;
        private String difficulty;
        private Double score;
        private LocalDateTime recommendedAt;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public Builder distance(Double distance) {
            this.distance = distance;
            return this;
        }

        public Builder priceLevel(String priceLevel) {
            this.priceLevel = priceLevel;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Builder recommendedAt(LocalDateTime recommendedAt) {
            this.recommendedAt = recommendedAt;
            return this;
        }

        public AIPersonalizedRecommendationItem build() {
            return new AIPersonalizedRecommendationItem(id, type, name, description, rating, distance, priceLevel, days, difficulty, score, recommendedAt);
        }
    }

}