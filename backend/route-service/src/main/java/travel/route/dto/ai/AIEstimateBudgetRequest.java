package travel.route.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AIEstimateBudgetRequest {

        @NotBlank(message = "目的地不能为空")
    private String destination;

        @Min(value = 1, message = "天数至少为1天")
    private Integer days;

    private Integer people;

    private Integer budget;

    private String style;

    public AIEstimateBudgetRequest() {
    }

    public AIEstimateBudgetRequest(String destination, Integer days, Integer people, Integer budget, String style) {
        this.destination = destination;
        this.days = days;
        this.people = people;
        this.budget = budget;
        this.style = style;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getPeople() {
        return people;
    }

    public void setPeople(Integer people) {
        this.people = people;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String destination;
        private Integer days;
        private Integer people;
        private Integer budget;
        private String style;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder people(Integer people) {
            this.people = people;
            return this;
        }

        public Builder budget(Integer budget) {
            this.budget = budget;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public AIEstimateBudgetRequest build() {
            return new AIEstimateBudgetRequest(destination, days, people, budget, style);
        }
    }

}