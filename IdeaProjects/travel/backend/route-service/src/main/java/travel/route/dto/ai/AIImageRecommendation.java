package travel.route.dto.ai;

import java.util.List;

public class AIImageRecommendation {

    private String type;

    private String name;

    private List<String> items;

    private List<String> tips;

    public AIImageRecommendation() {
    }

    public AIImageRecommendation(String type, String name, List<String> items, List<String> tips) {
        this.type = type;
        this.name = name;
        this.items = items;
        this.tips = tips;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String name;
        private List<String> items;
        private List<String> tips;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder items(List<String> items) {
            this.items = items;
            return this;
        }

        public Builder tips(List<String> tips) {
            this.tips = tips;
            return this;
        }

        public AIImageRecommendation build() {
            return new AIImageRecommendation(type, name, items, tips);
        }
    }

}