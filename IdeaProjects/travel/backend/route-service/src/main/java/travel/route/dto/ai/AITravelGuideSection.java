package travel.route.dto.ai;

import java.util.List;

/**
 * 旅游攻略的结构化内容。
 */
public class AITravelGuideSection {

    private List<String> preparationTips;
    private AITransportationGuide transportation;
    private List<AIDailyItinerary> dailyItineraries;
    private List<AIFoodRecommendation> foodRecommendations;
    private AIAccommodationGuide accommodation;
    private List<String> shoppingTips;
    private List<String> notes;

    public AITravelGuideSection() {
    }

    public AITravelGuideSection(List<String> preparationTips, AITransportationGuide transportation,
                                List<AIDailyItinerary> dailyItineraries,
                                List<AIFoodRecommendation> foodRecommendations,
                                AIAccommodationGuide accommodation, List<String> shoppingTips,
                                List<String> notes) {
        this.preparationTips = preparationTips;
        this.transportation = transportation;
        this.dailyItineraries = dailyItineraries;
        this.foodRecommendations = foodRecommendations;
        this.accommodation = accommodation;
        this.shoppingTips = shoppingTips;
        this.notes = notes;
    }

    public List<String> getPreparationTips() {
        return preparationTips;
    }

    public void setPreparationTips(List<String> preparationTips) {
        this.preparationTips = preparationTips;
    }

    public AITransportationGuide getTransportation() {
        return transportation;
    }

    public void setTransportation(AITransportationGuide transportation) {
        this.transportation = transportation;
    }

    public List<AIDailyItinerary> getDailyItineraries() {
        return dailyItineraries;
    }

    public void setDailyItineraries(List<AIDailyItinerary> dailyItineraries) {
        this.dailyItineraries = dailyItineraries;
    }

    public List<AIFoodRecommendation> getFoodRecommendations() {
        return foodRecommendations;
    }

    public void setFoodRecommendations(List<AIFoodRecommendation> foodRecommendations) {
        this.foodRecommendations = foodRecommendations;
    }

    public AIAccommodationGuide getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(AIAccommodationGuide accommodation) {
        this.accommodation = accommodation;
    }

    public List<String> getShoppingTips() {
        return shoppingTips;
    }

    public void setShoppingTips(List<String> shoppingTips) {
        this.shoppingTips = shoppingTips;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> preparationTips;
        private AITransportationGuide transportation;
        private List<AIDailyItinerary> dailyItineraries;
        private List<AIFoodRecommendation> foodRecommendations;
        private AIAccommodationGuide accommodation;
        private List<String> shoppingTips;
        private List<String> notes;

        public Builder preparationTips(List<String> preparationTips) {
            this.preparationTips = preparationTips;
            return this;
        }

        public Builder transportation(AITransportationGuide transportation) {
            this.transportation = transportation;
            return this;
        }

        public Builder dailyItineraries(List<AIDailyItinerary> dailyItineraries) {
            this.dailyItineraries = dailyItineraries;
            return this;
        }

        public Builder foodRecommendations(List<AIFoodRecommendation> foodRecommendations) {
            this.foodRecommendations = foodRecommendations;
            return this;
        }

        public Builder accommodation(AIAccommodationGuide accommodation) {
            this.accommodation = accommodation;
            return this;
        }

        public Builder shoppingTips(List<String> shoppingTips) {
            this.shoppingTips = shoppingTips;
            return this;
        }

        public Builder notes(List<String> notes) {
            this.notes = notes;
            return this;
        }

        public AITravelGuideSection build() {
            return new AITravelGuideSection(preparationTips, transportation, dailyItineraries,
                    foodRecommendations, accommodation, shoppingTips, notes);
        }
    }
}
