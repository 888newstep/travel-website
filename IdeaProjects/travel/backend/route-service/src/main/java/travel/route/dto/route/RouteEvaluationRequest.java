package travel.route.dto.route;

public class RouteEvaluationRequest {
    private String evaluationType;
    private Boolean includeDiversity;
    private Boolean includeReasonableness;
    private Boolean includeCostPerformance;

    public RouteEvaluationRequest() {}

    public String getEvaluationType() { return evaluationType; }
    public void setEvaluationType(String evaluationType) { this.evaluationType = evaluationType; }
    public Boolean getIncludeDiversity() { return includeDiversity; }
    public void setIncludeDiversity(Boolean includeDiversity) { this.includeDiversity = includeDiversity; }
    public Boolean getIncludeReasonableness() { return includeReasonableness; }
    public void setIncludeReasonableness(Boolean includeReasonableness) { this.includeReasonableness = includeReasonableness; }
    public Boolean getIncludeCostPerformance() { return includeCostPerformance; }
    public void setIncludeCostPerformance(Boolean includeCostPerformance) { this.includeCostPerformance = includeCostPerformance; }
}
