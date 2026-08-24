package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stable fields used by personalized route generation.
 * Unknown client fields are retained in extensions and are not used by the algorithm.
 */
public class PersonalizedRoutePreferences {

    @NotNull(message = "cityId is required")
    @Min(value = 1, message = "cityId must be positive")
    @Max(value = 100000000, message = "cityId is out of range")
    private Integer cityId;

    @Min(value = 1, message = "days must be at least 1")
    @Max(value = 30, message = "days must not exceed 30")
    private Integer days;

    @DecimalMin(value = "0.00", message = "budget must not be negative")
    @DecimalMax(value = "10000000.00", message = "budget is out of range")
    @Digits(integer = 8, fraction = 2, message = "budget supports at most 2 decimal places")
    private BigDecimal budget;

    @Size(max = 32, message = "preference must not exceed 32 characters")
    private String preference;

    @Size(max = 20, message = "interests must contain at most 20 items")
    private List<@NotBlank(message = "interest must not be blank")
            @Size(max = 64, message = "interest must not exceed 64 characters") String> interests;

    @Size(max = 32, message = "transportPreference must not exceed 32 characters")
    private String transportPreference;

    @Size(max = 20, message = "extensions must contain at most 20 fields")
    private Map<String, JsonNode> extensions;

    public PersonalizedRoutePreferences() {
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public String getTransportPreference() {
        return transportPreference;
    }

    public void setTransportPreference(String transportPreference) {
        this.transportPreference = transportPreference;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= 20) {
            throw new IllegalArgumentException("extensions must contain at most 20 fields");
        }
        extensions.put(name, value);
    }

    public PersonalizedRoutePreferences copy() {
        PersonalizedRoutePreferences copy = new PersonalizedRoutePreferences();
        copy.cityId = cityId;
        copy.days = days;
        copy.budget = budget;
        copy.preference = preference;
        copy.interests = interests == null ? null : new ArrayList<>(interests);
        copy.transportPreference = transportPreference;
        copy.extensions = extensions == null ? null : new LinkedHashMap<>(extensions);
        return copy;
    }

    private void mergeExtensions(Map<String, JsonNode> additionalExtensions) {
        if (additionalExtensions == null || additionalExtensions.isEmpty()) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : additionalExtensions.entrySet()) {
            addExtension(entry.getKey(), entry.getValue());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer cityId;
        private Integer days;
        private BigDecimal budget;
        private String preference;
        private List<String> interests;
        private String transportPreference;
        private Map<String, JsonNode> extensions;

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder budget(BigDecimal budget) {
            this.budget = budget;
            return this;
        }

        public Builder preference(String preference) {
            this.preference = preference;
            return this;
        }

        public Builder interests(List<String> interests) {
            this.interests = interests;
            return this;
        }

        public Builder transportPreference(String transportPreference) {
            this.transportPreference = transportPreference;
            return this;
        }

        public Builder extensions(Map<String, JsonNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public PersonalizedRoutePreferences build() {
            PersonalizedRoutePreferences result = new PersonalizedRoutePreferences();
            result.cityId = cityId;
            result.days = days;
            result.budget = budget;
            result.preference = preference;
            result.interests = interests;
            result.transportPreference = transportPreference;
            result.setExtensions(extensions);
            return result;
        }
    }
}
