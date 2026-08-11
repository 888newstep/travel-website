package travel.route.dto.ai;

public class AIActivity {

    private String time;

    private String type;

    private String name;

    private String description;

    public AIActivity() {
    }

    public AIActivity(String time, String type, String name, String description) {
        this.time = time;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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
        private String time;
        private String type;
        private String name;
        private String description;

        public Builder time(String time) {
            this.time = time;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public AIActivity build() {
            return new AIActivity(time, type, name, description);
        }
    }

}