package travel.route.dto.ai;

public class AIRecommendationItem {

    private Integer id;

    private String name;

    private String description;

    private Integer matchScore;

    private String source;

    public AIRecommendationItem() {
    }

    public AIRecommendationItem(Integer id, String name, String description, Integer matchScore, String source) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.matchScore = matchScore;
        this.source = source;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String name;
        private String description;
        private Integer matchScore;
        private String source;

        public Builder id(Integer id) {
            this.id = id;
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

        public Builder matchScore(Integer matchScore) {
            this.matchScore = matchScore;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIRecommendationItem build() {
            return new AIRecommendationItem(id, name, description, matchScore, source);
        }
    }

}