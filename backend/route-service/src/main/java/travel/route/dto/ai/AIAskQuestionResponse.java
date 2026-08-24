package travel.route.dto.ai;

import java.time.LocalDateTime;

public class AIAskQuestionResponse {

    private String question;

    private String answer;

    private Double confidence;

    private LocalDateTime timestamp;

    private String source;

    public AIAskQuestionResponse() {
    }

    public AIAskQuestionResponse(String question, String answer, Double confidence, LocalDateTime timestamp, String source) {
        this.question = question;
        this.answer = answer;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.source = source;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
        private String question;
        private String answer;
        private Double confidence;
        private LocalDateTime timestamp;
        private String source;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIAskQuestionResponse build() {
            return new AIAskQuestionResponse(question, answer, confidence, timestamp, source);
        }
    }

}