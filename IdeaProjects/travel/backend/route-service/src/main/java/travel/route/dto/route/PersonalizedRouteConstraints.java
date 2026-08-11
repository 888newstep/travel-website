package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constraints accepted by the personalized route boundary.
 * The current algorithm only transports these values; future supported constraints
 * must become explicit fields with their own behavior and tests.
 */
public class PersonalizedRouteConstraints {

    @Size(max = 20, message = "extensions must contain at most 20 fields")
    private Map<String, JsonNode> extensions;

    public PersonalizedRouteConstraints() {
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

    public PersonalizedRouteConstraints copy() {
        PersonalizedRouteConstraints copy = new PersonalizedRouteConstraints();
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
        private Map<String, JsonNode> extensions;

        public Builder extensions(Map<String, JsonNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public PersonalizedRouteConstraints build() {
            PersonalizedRouteConstraints result = new PersonalizedRouteConstraints();
            result.setExtensions(extensions);
            return result;
        }
    }
}
