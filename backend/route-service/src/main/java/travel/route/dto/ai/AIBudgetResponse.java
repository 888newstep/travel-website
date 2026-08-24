package travel.route.dto.ai;

public class AIBudgetResponse {

    private String destination;

    private Integer days;

    private Integer people;

    private String totalBudget;

    private String breakdown;

    private String advice;

    private String source;

    private AIBudgetDetails details;

    public AIBudgetResponse() {
    }

    public AIBudgetResponse(String destination, Integer days, Integer people, String totalBudget, String breakdown, String advice, String source, AIBudgetDetails details) {
        this.destination = destination;
        this.days = days;
        this.people = people;
        this.totalBudget = totalBudget;
        this.breakdown = breakdown;
        this.advice = advice;
        this.source = source;
        this.details = details;
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

    public String getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(String totalBudget) {
        this.totalBudget = totalBudget;
    }

    public String getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(String breakdown) {
        this.breakdown = breakdown;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public AIBudgetDetails getDetails() {
        return details;
    }

    public void setDetails(AIBudgetDetails details) {
        this.details = details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String destination;
        private Integer days;
        private Integer people;
        private String totalBudget;
        private String breakdown;
        private String advice;
        private String source;
        private AIBudgetDetails details;

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

        public Builder totalBudget(String totalBudget) {
            this.totalBudget = totalBudget;
            return this;
        }

        public Builder breakdown(String breakdown) {
            this.breakdown = breakdown;
            return this;
        }

        public Builder advice(String advice) {
            this.advice = advice;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder details(AIBudgetDetails details) {
            this.details = details;
            return this;
        }

        public AIBudgetResponse build() {
            return new AIBudgetResponse(destination, days, people, totalBudget, breakdown, advice, source, details);
        }
    }

}
