package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class AIAssistantChatRequest {

        @NotBlank(message = "查询内容不能为空")
    private String query;

    private Map<String, Object> context;

    public AIAssistantChatRequest() {
    }

    public AIAssistantChatRequest(String query, Map<String, Object> context) {
        this.query = query;
        this.context = context;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private Map<String, Object> context;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public AIAssistantChatRequest build() {
            return new AIAssistantChatRequest(query, context);
        }
    }

}