package travel.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpKeyResolverTest {
    private final ClientIpKeyResolver resolver = new ClientIpKeyResolver();

    @Test
    void shouldResolveRemoteAddressAsKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/routes")
                        .remoteAddress(new InetSocketAddress("203.0.113.7", 51000))
                        .build());

        assertEquals("203.0.113.7", resolver.resolve(exchange).block());
    }

    @Test
    void shouldNotTrustForwardedForHeader() {
        // X-Forwarded-For 可被客户端伪造，resolver 必须只看 remoteAddress
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/login")
                        .remoteAddress(new InetSocketAddress("198.51.100.9", 51000))
                        .header("X-Forwarded-For", "1.2.3.4")
                        .build());

        assertEquals("198.51.100.9", resolver.resolve(exchange).block());
    }
}
