package travel.route.dto.ai;

public class AIImageQualityAnalysis {

    private Double sharpness;

    private Double brightness;

    private Double contrast;

    private Double composition;

    private Double overallQuality;

    public AIImageQualityAnalysis() {
    }

    public AIImageQualityAnalysis(Double sharpness, Double brightness, Double contrast, Double composition, Double overallQuality) {
        this.sharpness = sharpness;
        this.brightness = brightness;
        this.contrast = contrast;
        this.composition = composition;
        this.overallQuality = overallQuality;
    }

    public Double getSharpness() {
        return sharpness;
    }

    public void setSharpness(Double sharpness) {
        this.sharpness = sharpness;
    }

    public Double getBrightness() {
        return brightness;
    }

    public void setBrightness(Double brightness) {
        this.brightness = brightness;
    }

    public Double getContrast() {
        return contrast;
    }

    public void setContrast(Double contrast) {
        this.contrast = contrast;
    }

    public Double getComposition() {
        return composition;
    }

    public void setComposition(Double composition) {
        this.composition = composition;
    }

    public Double getOverallQuality() {
        return overallQuality;
    }

    public void setOverallQuality(Double overallQuality) {
        this.overallQuality = overallQuality;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double sharpness;
        private Double brightness;
        private Double contrast;
        private Double composition;
        private Double overallQuality;

        public Builder sharpness(Double sharpness) {
            this.sharpness = sharpness;
            return this;
        }

        public Builder brightness(Double brightness) {
            this.brightness = brightness;
            return this;
        }

        public Builder contrast(Double contrast) {
            this.contrast = contrast;
            return this;
        }

        public Builder composition(Double composition) {
            this.composition = composition;
            return this;
        }

        public Builder overallQuality(Double overallQuality) {
            this.overallQuality = overallQuality;
            return this;
        }

        public AIImageQualityAnalysis build() {
            return new AIImageQualityAnalysis(sharpness, brightness, contrast, composition, overallQuality);
        }
    }

}