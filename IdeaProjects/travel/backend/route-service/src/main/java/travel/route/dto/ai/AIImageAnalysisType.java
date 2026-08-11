package travel.route.dto.ai;

public class AIImageAnalysisType {

    private String value;

    private String label;

    public AIImageAnalysisType() {
    }

    public AIImageAnalysisType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String value;
        private String label;

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public AIImageAnalysisType build() {
            return new AIImageAnalysisType(value, label);
        }
    }

}