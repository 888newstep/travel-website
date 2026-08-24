package travel.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;

class AuthGlobalFilterTest {

    @Test
    void shouldFailFastWhenJwtSecretIsMissing() {
        AuthGlobalFilter filter = new AuthGlobalFilter();

        assertThrows(IllegalStateException.class, filter::validateJwtSecret);
    }

    @Test
    void shouldFailFastWhenJwtSecretIsTooShort() {
        AuthGlobalFilter filter = new AuthGlobalFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", "too-short");

        assertThrows(IllegalStateException.class, filter::validateJwtSecret);
    }

    @Test
    void shouldAllowPublicReadRequest() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(get("/api/routes/42").build());
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.filter(exchange, currentExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void shouldAllowPublicRouteScheduleAndTrafficReads() {
        AuthGlobalFilter filter = configuredFilter();

        for (String path : new String[]{"/api/routes/42/schedule", "/api/routes/42/traffic"}) {
            ServerWebExchange exchange = MockServerWebExchange.from(get(path).build());
            AtomicBoolean chainCalled = new AtomicBoolean();

            filter.filter(exchange, currentExchange -> {
                chainCalled.set(true);
                return Mono.empty();
            }).block();

            assertTrue(chainCalled.get(), path);
        }
    }

    @Test
    void shouldAllowPublicTravelNoteViewCounter() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
                post("/api/travel-notes/42/view").build());
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.filter(exchange, currentExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void shouldRemoveSpoofedIdentityHeadersFromPublicRequests() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
                get("/api/ai/chat")
                        .header("X-User-Id", "999")
                        .header("X-User-Type", "9")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-Client-IP", "203.0.113.8")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, currentExchange -> {
            forwarded.set(currentExchange);
            return Mono.empty();
        }).block();

        assertFalse(forwarded.get().getRequest().getHeaders().containsKey("X-User-Id"));
        assertFalse(forwarded.get().getRequest().getHeaders().containsKey("X-User-Type"));
        assertFalse(forwarded.get().getRequest().getHeaders().containsKey("X-User-Role"));
        assertEquals("unknown", forwarded.get().getRequest().getHeaders().getFirst("X-Client-IP"));
    }

    @Test
    void shouldAllowPublicShareAccessButProtectShareManagement() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange accessExchange = MockServerWebExchange.from(
                get("/api/route-share/access/ABC123").build());
        AtomicBoolean accessChainCalled = new AtomicBoolean();

        filter.filter(accessExchange, currentExchange -> {
            accessChainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(accessChainCalled.get());

        ServerWebExchange managementExchange = MockServerWebExchange.from(
                get("/api/route-share/statistics/8").build());
        filter.filter(managementExchange, currentExchange -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, managementExchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectProtectedWriteWithoutToken() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(post("/api/routes").build());
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.filter(exchange, currentExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldNotExposeManagementEndpoints() {
        AuthGlobalFilter filter = configuredFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(get("/actuator/env").build());

        filter.filter(exchange, currentExchange -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    private AuthGlobalFilter configuredFilter() {
        AuthGlobalFilter filter = new AuthGlobalFilter();
        ReflectionTestUtils.setField(
                filter,
                "jwtSecret",
                "test-jwt-secret-that-is-long-enough-for-hmac-signing");
        filter.validateJwtSecret();
        return filter;
    }
}
