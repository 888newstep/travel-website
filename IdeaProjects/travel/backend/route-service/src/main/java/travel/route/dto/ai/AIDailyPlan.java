package travel.route.dto.ai;

import java.util.List;

public class AIDailyPlan {

    private Integer day;

    private String title;

    private List<AIActivity> activities;

    public AIDailyPlan() {
    }

    public AIDailyPlan(Integer day, String title, List<AIActivity> activities) {
        this.day = day;
        this.title = title;
        this.activities = activities;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<AIActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<AIActivity> activities) {
        this.activities = activities;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer day;
        private String title;
        private List<AIActivity> activities;

        public Builder day(Integer day) {
            this.day = day;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder activities(List<AIActivity> activities) {
            this.activities = activities;
            return this;
        }

        public AIDailyPlan build() {
            return new AIDailyPlan(day, title, activities);
        }
    }

}