package travel.route.dto.ai;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 问答请求，替代控制器层的 Map 入参。
 */
public class AIAskQuestionRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer userId;

    public AIAskQuestionRequest() {
    }

    public AIAskQuestionRequest(String question, Integer userId) {
        this.question = question;
        this.userId = userId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
