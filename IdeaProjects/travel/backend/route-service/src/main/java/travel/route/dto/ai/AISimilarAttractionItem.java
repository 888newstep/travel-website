package travel.route.dto.ai;

public class AISimilarAttractionItem {

    private Integer id;

    private String name;

    private String description;

    private Double score;

    public AISimilarAttractionItem() {
    }

    public AISimilarAttractionItem(Integer id, String name, String description, Double score) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.score = score;
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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String name;
        private String description;
        private Double score;

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

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public AISimilarAttractionItem build() {
            return new AISimilarAttractionItem(id, name, description, score);
        }
    }

}