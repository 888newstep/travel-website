package travel.route.dto.route;

/** 雨天等场景下的替代景点。 */
public class RealTimeAlternativeAttraction {

    private Integer attractionId;
    private String attractionName;
    private String description;
    private String type;

    public RealTimeAlternativeAttraction() {
    }

    public RealTimeAlternativeAttraction(Integer attractionId, String attractionName,
                                         String description, String type) {
        this.attractionId = attractionId;
        this.attractionName = attractionName;
        this.description = description;
        this.type = type;
    }

    public Integer getAttractionId() {
        return attractionId;
    }

    public void setAttractionId(Integer attractionId) {
        this.attractionId = attractionId;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
