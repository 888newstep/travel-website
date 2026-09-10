package travel.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;

class SecurityHeadersGlobalFilterTest {
    @Test
    void shouldAddSecurityHeadersAndRemoveFingerprints() {
        SecurityHeadersGlobalFilter filter = new SecurityHeadersGlobalFilter(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(get("/api/routes").build());
        exchange.getResponse().getHeaders().set("Server", "Netty");
        exchange.getResponse().getHeaders().set("X-Powered-By", "Spring");
        exchange.getResponse().getHeaders().set("X-Application-Context", "gateway");

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin",
                exchange.getResponse().getHeaders().getFirst("Referrer-Policy"));
        assertEquals("default-src 'none'; frame-ancestors 'none'; base-uri 'none'",
                exchange.getResponse().getHeaders().getFirst("Content-Security-Policy"));
        assertFalse(exchange.getResponse().getHeaders().containsKey("Strict-Transport-Security"));
        assertFalse(exchange.getResponse().getHeaders().containsKey("Server"));
        assertFalse(exchange.getResponse().getHeaders().containsKey("X-Powered-By"));
        assertFalse(exchange.getResponse().getHeaders().containsKey("X-Application-Context"));
    }

    @Test
    void shouldEnableHstsOnlyWhenConfigured() {
        SecurityHeadersGlobalFilter filter = new SecurityHeadersGlobalFilter(true);
        MockServerWebExchange exchange = MockServerWebExchange.from(get("/api/routes").build());

        filter.filter(exchange, current -> current.getResponse().setComplete()).block();

        assertEquals("max-age=31536000; includeSubDomains",
                exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }
}
