package travel.route.dto.route;

import java.math.BigDecimal;
import java.util.List;

public class RoutePreferenceRequest {
    private List<String> preferredTypes;
    private BigDecimal budget;
    private String transportPreference;
    private Integer days;
    private Integer cityId;

    public RoutePreferenceRequest() {}

    public List<String> getPreferredTypes() { return preferredTypes; }
    public void setPreferredTypes(List<String> preferredTypes) { this.preferredTypes = preferredTypes; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getTransportPreference() { return transportPreference; }
    public void setTransportPreference(String transportPreference) { this.transportPreference = transportPreference; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }
}
