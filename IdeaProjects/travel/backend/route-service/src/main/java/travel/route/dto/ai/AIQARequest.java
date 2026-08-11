package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;

public class AIQARequest {

        @NotBlank(message = "问题不能为空")
    private String question;

    public AIQARequest() {
    }

    public AIQARequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String question;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public AIQARequest build() {
            return new AIQARequest(question);
        }
    }

}