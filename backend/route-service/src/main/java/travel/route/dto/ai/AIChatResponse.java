package travel.route.dto.ai;

public class AIChatResponse {

    private String response;

    private String source;

    private String conversationId;

    private Long timestamp;

    public AIChatResponse() {
    }

    public AIChatResponse(String response, String source, String conversationId, Long timestamp) {
        this.response = response;
        this.source = source;
        this.conversationId = conversationId;
        this.timestamp = timestamp;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String response;
        private String source;
        private String conversationId;
        private Long timestamp;

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AIChatResponse build() {
            return new AIChatResponse(response, source, conversationId, timestamp);
        }
    }

}