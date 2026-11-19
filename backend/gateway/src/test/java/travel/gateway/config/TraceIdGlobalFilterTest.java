package travel.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdGlobalFilterTest {
    private final TraceIdGlobalFilter filter = new TraceIdGlobalFilter();

    @Test
    void shouldGenerateTraceIdForwardDownstreamAndEchoResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/routes").build());
        AtomicReference<String> downstream = new AtomicReference<>();

        filter.filter(exchange, current -> {
            downstream.set(current.getRequest().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_HEADER));
            return current.getResponse().setComplete();
        }).block();

        assertNotNull(downstream.get());
        assertEquals(32, downstream.get().length());
        assertTrue(downstream.get().matches("[0-9a-f]{32}"));
        assertEquals(downstream.get(), exchange.getResponse().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_HEADER));
    }

    @Test
    void shouldNotTrustExternalTraceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/routes")
                        .header(TraceIdGlobalFilter.TRACE_HEADER, "forged-id\ninjected")
                        .build());
        AtomicReference<String> downstream = new AtomicReference<>();

        filter.filter(exchange, current -> {
            downstream.set(current.getRequest().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_HEADER));
            return current.getResponse().setComplete();
        }).block();

        assertNotNull(downstream.get());
        assertNotEquals("forged-id\ninjected", downstream.get());
    }
}
