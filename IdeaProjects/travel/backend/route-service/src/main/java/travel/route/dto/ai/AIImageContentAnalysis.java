package travel.route.dto.ai;

import java.util.List;

public class AIImageContentAnalysis {

    private String mainSubject;

    private List<String> objects;

    private String sceneType;

    private List<String> dominantColors;

    private String season;

    public AIImageContentAnalysis() {
    }

    public AIImageContentAnalysis(String mainSubject, List<String> objects, String sceneType, List<String> dominantColors, String season) {
        this.mainSubject = mainSubject;
        this.objects = objects;
        this.sceneType = sceneType;
        this.dominantColors = dominantColors;
        this.season = season;
    }

    public String getMainSubject() {
        return mainSubject;
    }

    public void setMainSubject(String mainSubject) {
        this.mainSubject = mainSubject;
    }

    public List<String> getObjects() {
        return objects;
    }

    public void setObjects(List<String> objects) {
        this.objects = objects;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public List<String> getDominantColors() {
        return dominantColors;
    }

    public void setDominantColors(List<String> dominantColors) {
        this.dominantColors = dominantColors;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String mainSubject;
        private List<String> objects;
        private String sceneType;
        private List<String> dominantColors;
        private String season;

        public Builder mainSubject(String mainSubject) {
            this.mainSubject = mainSubject;
            return this;
        }

        public Builder objects(List<String> objects) {
            this.objects = objects;
            return this;
        }

        public Builder sceneType(String sceneType) {
            this.sceneType = sceneType;
            return this;
        }

        public Builder dominantColors(List<String> dominantColors) {
            this.dominantColors = dominantColors;
            return this;
        }

        public Builder season(String season) {
            this.season = season;
            return this;
        }

        public AIImageContentAnalysis build() {
            return new AIImageContentAnalysis(mainSubject, objects, sceneType, dominantColors, season);
        }
    }

}