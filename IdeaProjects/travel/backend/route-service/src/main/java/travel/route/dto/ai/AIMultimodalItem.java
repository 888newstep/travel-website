package travel.route.dto.ai;

public class AIMultimodalItem {

    private Integer id;

    private String title;

    private String description;

    private Double score;

    private Double relevance;

    public AIMultimodalItem() {
    }

    public AIMultimodalItem(Integer id, String title, String description, Double score, Double relevance) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.score = score;
        this.relevance = relevance;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getRelevance() {
        return relevance;
    }

    public void setRelevance(Double relevance) {
        this.relevance = relevance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String title;
        private String description;
        private Double score;
        private Double relevance;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Builder relevance(Double relevance) {
            this.relevance = relevance;
            return this;
        }

        public AIMultimodalItem build() {
            return new AIMultimodalItem(id, title, description, score, relevance);
        }
    }

}