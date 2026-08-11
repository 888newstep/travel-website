package travel.route.dto.ai;

public class AIOptimizeSuggestion {

    private String type;

    private String description;

    public AIOptimizeSuggestion() {
    }

    public AIOptimizeSuggestion(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String description;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public AIOptimizeSuggestion build() {
            return new AIOptimizeSuggestion(type, description);
        }
    }

}