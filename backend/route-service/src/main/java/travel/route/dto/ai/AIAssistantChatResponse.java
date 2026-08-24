package travel.route.dto.ai;

import java.util.List;

public class AIAssistantChatResponse {

    private String response;

    private List<String> suggestions;

    private String source;

    public AIAssistantChatResponse() {
    }

    public AIAssistantChatResponse(String response, List<String> suggestions, String source) {
        this.response = response;
        this.suggestions = suggestions;
        this.source = source;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
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
        private String response;
        private List<String> suggestions;
        private String source;

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIAssistantChatResponse build() {
            return new AIAssistantChatResponse(response, suggestions, source);
        }
    }

}