package travel.route.dto.ai;

/**
 * 旅游攻略中的单日行程。
 */
public class AIDailyItinerary {

    private Integer day;
    private String title;
    private String description;

    public AIDailyItinerary() {
    }

    public AIDailyItinerary(Integer day, String title, String description) {
        this.day = day;
        this.title = title;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer day;
        private String title;
        private String description;

        public Builder day(Integer day) {
            this.day = day;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public AIDailyItinerary build() {
            return new AIDailyItinerary(day, title, description);
        }
    }
}
