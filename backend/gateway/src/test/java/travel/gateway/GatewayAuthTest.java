package travel.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * ⚠️ 已跳过 — 依赖完整 Spring Cloud Gateway 上下文（Nacos、Sentinel Dashboard）。
 * 认证逻辑已由 AuthGlobalFilterUnitTest 覆盖，无需在此运行。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Disabled("Requires running Nacos and downstream services")
public class GatewayAuthTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testPublicEndpointWithoutToken() {
        webTestClient.get().uri("/api/routes/public")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testProtectedEndpointWithoutToken() {
        webTestClient.get().uri("/api/routes/my-routes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void testProtectedEndpointWithInvalidToken() {
        webTestClient.get().uri("/api/routes/my-routes")
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
