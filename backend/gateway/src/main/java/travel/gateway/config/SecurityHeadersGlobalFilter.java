package travel.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 为所有 Gateway 响应统一添加安全基线头并移除框架指纹。
 * 前端静态资源由独立 Web 容器提供，因此 API CSP 可以采用严格策略。
 */
@Component
public class SecurityHeadersGlobalFilter implements GlobalFilter, Ordered {
    private static final String PERMISSIONS_POLICY =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    private final boolean hstsEnabled;

    public SecurityHeadersGlobalFilter(
            @Value("${travel.security.headers.hsts-enabled:false}") boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
            headers.set("Permissions-Policy", PERMISSIONS_POLICY);
            headers.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            if (hstsEnabled) {
                headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            } else {
                headers.remove("Strict-Transport-Security");
            }
            headers.remove("Server");
            headers.remove("X-Powered-By");
            headers.remove("X-Application-Context");
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
