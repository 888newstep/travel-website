package travel.route.dto.optimization;

public class OptimizationSuggestion {
    private Integer id;
    private String title;
    private String description;
    private String type;
    private String message;

    public OptimizationSuggestion() {}
    public OptimizationSuggestion(String type, String message) {
        this.type = type;
        this.message = message;
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Integer id;
        private String title;
        private String description;
        private String type;
        private String message;
        public Builder id(Integer id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public OptimizationSuggestion build() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion(type, message);
            suggestion.setId(id);
            suggestion.setTitle(title);
            suggestion.setDescription(description);
            return suggestion;
        }
    }
}
