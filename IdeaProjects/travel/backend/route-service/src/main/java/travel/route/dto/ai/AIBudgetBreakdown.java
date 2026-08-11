package travel.route.dto.ai;

/**
 * 预算分类明细，金额单位为元。
 */
public class AIBudgetBreakdown {

    private Double accommodation;
    private Double transportation;
    private Double food;
    private Double attractions;
    private Double shopping;
    private Double miscellaneous;
    private Double total;

    public AIBudgetBreakdown() {
    }

    public AIBudgetBreakdown(Double accommodation, Double transportation, Double food,
                             Double attractions, Double shopping, Double miscellaneous,
                             Double total) {
        this.accommodation = accommodation;
        this.transportation = transportation;
        this.food = food;
        this.attractions = attractions;
        this.shopping = shopping;
        this.miscellaneous = miscellaneous;
        this.total = total;
    }

    public Double getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(Double accommodation) {
        this.accommodation = accommodation;
    }

    public Double getTransportation() {
        return transportation;
    }

    public void setTransportation(Double transportation) {
        this.transportation = transportation;
    }

    public Double getFood() {
        return food;
    }

    public void setFood(Double food) {
        this.food = food;
    }

    public Double getAttractions() {
        return attractions;
    }

    public void setAttractions(Double attractions) {
        this.attractions = attractions;
    }

    public Double getShopping() {
        return shopping;
    }

    public void setShopping(Double shopping) {
        this.shopping = shopping;
    }

    public Double getMiscellaneous() {
        return miscellaneous;
    }

    public void setMiscellaneous(Double miscellaneous) {
        this.miscellaneous = miscellaneous;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double accommodation;
        private Double transportation;
        private Double food;
        private Double attractions;
        private Double shopping;
        private Double miscellaneous;
        private Double total;

        public Builder accommodation(Double accommodation) {
            this.accommodation = accommodation;
            return this;
        }

        public Builder transportation(Double transportation) {
            this.transportation = transportation;
            return this;
        }

        public Builder food(Double food) {
            this.food = food;
            return this;
        }

        public Builder attractions(Double attractions) {
            this.attractions = attractions;
            return this;
        }

        public Builder shopping(Double shopping) {
            this.shopping = shopping;
            return this;
        }

        public Builder miscellaneous(Double miscellaneous) {
            this.miscellaneous = miscellaneous;
            return this;
        }

        public Builder total(Double total) {
            this.total = total;
            return this;
        }

        public AIBudgetBreakdown build() {
            return new AIBudgetBreakdown(accommodation, transportation, food, attractions,
                    shopping, miscellaneous, total);
        }
    }
}
