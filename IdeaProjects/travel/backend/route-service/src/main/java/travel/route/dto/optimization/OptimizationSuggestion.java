package travel.route.dto.optimization;

public class OptimizationSuggestion {
    private String type;
    private String message;

    public OptimizationSuggestion() {}
    public OptimizationSuggestion(String type, String message) {
        this.type = type;
        this.message = message;
    }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String type;
        private String message;
        public Builder type(String type) { this.type = type; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public OptimizationSuggestion build() { return new OptimizationSuggestion(type, message); }
    }
}