package travel.route.dto.ai;

import java.util.Map;

public class AIMultimodalQueryRequest {

    private String text;

    private String image;

    private Map<String, Object> context;

    public AIMultimodalQueryRequest() {
    }

    public AIMultimodalQueryRequest(String text, String image, Map<String, Object> context) {
        this.text = text;
        this.image = image;
        this.context = context;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private String image;
        private Map<String, Object> context;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public AIMultimodalQueryRequest build() {
            return new AIMultimodalQueryRequest(text, image, context);
        }
    }

}