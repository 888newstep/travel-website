package travel.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebMvcConfigTest {
    @Test
    void shouldParseExplicitOrigins() {
        assertArrayEquals(new String[]{"https://travel.example.com", "http://localhost:5173"},
                WebMvcConfig.parseAllowedOrigins(
                        " https://travel.example.com,http://localhost:5173,https://travel.example.com "));
    }

    @Test
    void shouldRejectWildcardOrEmptyOrigins() {
        assertThrows(IllegalStateException.class, () -> WebMvcConfig.parseAllowedOrigins("*"));
        assertThrows(IllegalStateException.class, () -> WebMvcConfig.parseAllowedOrigins("https://*.example.com"));
        assertThrows(IllegalStateException.class, () -> WebMvcConfig.parseAllowedOrigins(""));
    }
}
