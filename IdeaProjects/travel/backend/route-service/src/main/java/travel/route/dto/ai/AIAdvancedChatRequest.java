package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AIAdvancedChatRequest {

        @NotBlank(message = "消息内容不能为空")
    private String message;

    private String conversationId;

    public AIAdvancedChatRequest() {
    }

    public AIAdvancedChatRequest(String message, String conversationId) {
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String conversationId;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public AIAdvancedChatRequest build() {
            return new AIAdvancedChatRequest(message, conversationId);
        }
    }

}