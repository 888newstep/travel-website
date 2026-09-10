package travel.gateway.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayCorsConfigTest {
    @Test
    void shouldParseExplicitOrigins() {
        assertEquals(List.of("https://travel.example.com", "http://localhost:5173"),
                GatewayCorsConfig.parseAllowedOrigins(
                        " https://travel.example.com, http://localhost:5173,https://travel.example.com "));
    }

    @Test
    void shouldRejectWildcardOrEmptyOrigins() {
        assertThrows(IllegalStateException.class, () -> GatewayCorsConfig.parseAllowedOrigins("*"));
        assertThrows(IllegalStateException.class, () -> GatewayCorsConfig.parseAllowedOrigins("https://*.example.com"));
        assertThrows(IllegalStateException.class, () -> GatewayCorsConfig.parseAllowedOrigins("  "));
    }
}
