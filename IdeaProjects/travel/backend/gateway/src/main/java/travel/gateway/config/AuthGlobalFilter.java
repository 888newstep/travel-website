package travel.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private static final List<String> WHITE_LIST = List.of(
            "/api/users/login", "/api/users/register", "/api/users/captcha",
            "/api/attractions/**", "/api/cities/**", "/api/restaurants/**", "/api/realtime-status/**",
            "/api/routes/**", "/api/travel-notes/**", "/api/ai/**", "/api/route-share/**",
            "/swagger-ui/**", "/v3/api-docs/**", "/doc.html",
            "/actuator/**"
    );

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private volatile boolean secretConfigured;

    @jakarta.annotation.PostConstruct
    void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.warn("JWT secret is not configured — authentication will pass all requests. "
                    + "Set JWT_SECRET environment variable before deploying to production.");
            secretConfigured = false;
        } else {
            secretConfigured = true;
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip authentication if JWT secret is not configured (e.g. test / local dev)
        if (!secretConfigured) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (WHITE_LIST.stream().anyMatch(pattern -> matchPath(path, pattern))) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtSecret.getBytes(StandardCharsets.UTF_8)));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object userId = claims.get("userId");
            Object userType = claims.get("userType");
            Object role = claims.get("role");

            exchange.getRequest().mutate()
                    .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                    .header("X-User-Type", userType == null ? "" : String.valueOf(userType))
                    .header("X-User-Role", String.valueOf(role != null ? role : userType));
            return chain.filter(exchange);
        } catch (Exception exception) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean matchPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        return path.equals(pattern);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
