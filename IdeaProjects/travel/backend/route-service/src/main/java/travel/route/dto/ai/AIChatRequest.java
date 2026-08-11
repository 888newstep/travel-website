package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AIChatRequest {

        @NotBlank(message = "消息内容不能为空")
    private String message;

    private String systemPrompt;

    public AIChatRequest() {
    }

    public AIChatRequest(String message, String systemPrompt) {
        this.message = message;
        this.systemPrompt = systemPrompt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String systemPrompt;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public AIChatRequest build() {
            return new AIChatRequest(message, systemPrompt);
        }
    }

}