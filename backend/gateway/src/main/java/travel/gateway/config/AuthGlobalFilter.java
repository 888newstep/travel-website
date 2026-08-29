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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_TYPE_HEADER = "X-User-Type";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String CLIENT_IP_HEADER = "X-Client-IP";

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/users/login", "/api/users/register", "/api/users/captcha"
    );
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/attractions", "/api/cities", "/api/restaurants", "/api/routes",
            "/api/routes/search", "/api/travel-notes", "/api/route-share/validate",
            "/swagger-ui", "/swagger-ui.html",
            "/v3/api-docs", "/doc.html", "/actuator/health"
    );
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/attractions/", "/api/cities/", "/api/restaurants/", "/api/realtime-status/",
            "/api/routes/city/", "/api/travel-notes/", "/swagger-ui/", "/v3/api-docs/",
            "/api/route-share/info/", "/api/route-share/access/", "/api/route-share/file/access/",
            "/actuator/health/"
    );

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private volatile boolean secretConfigured;

    @jakarta.annotation.PostConstruct
    void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured before the gateway can start");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        secretConfigured = true;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!secretConfigured) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }

        exchange = sanitizeForwardedHeaders(exchange);
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicRequest(exchange, path)) {
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

            ServerWebExchange authenticatedExchange = exchange.mutate()
                    .request(builder -> builder.headers(headers -> {
                        headers.set(USER_ID_HEADER, userId == null ? "" : String.valueOf(userId));
                        headers.set(USER_TYPE_HEADER, userType == null ? "" : String.valueOf(userType));
                        headers.set(USER_ROLE_HEADER, String.valueOf(role != null ? role : userType));
                    }))
                    .build();
            return chain.filter(authenticatedExchange);
        } catch (Exception exception) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private ServerWebExchange sanitizeForwardedHeaders(ServerWebExchange exchange) {
        String clientIp = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_TYPE_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(CLIENT_IP_HEADER);
                    headers.set(CLIENT_IP_HEADER, clientIp);
                }))
                .build();
    }

    private boolean isPublicRequest(ServerWebExchange exchange, String path) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        if (HttpMethod.POST.equals(method) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }
        if (HttpMethod.POST.equals(method) && path.startsWith("/api/route-share/visit/")) {
            return true;
        }
        if (HttpMethod.POST.equals(method) && isPublicTravelNoteView(path)) {
            return true;
        }
        if (!HttpMethod.GET.equals(method)) {
            return false;
        }
        return PUBLIC_GET_PATHS.contains(path)
                || PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith)
                || isPublicRouteRead(path);
    }

    private boolean isPublicRouteRead(String path) {
        String prefix = "/api/routes/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String routePath = path.substring(prefix.length());
        String routeId = routePath;
        for (String publicSuffix : List.of("/schedule", "/traffic")) {
            if (routePath.endsWith(publicSuffix)) {
                routeId = routePath.substring(0, routePath.length() - publicSuffix.length());
                break;
            }
        }
        return !routeId.isEmpty() && routeId.chars().allMatch(Character::isDigit);
    }

    private boolean isPublicTravelNoteView(String path) {
        String prefix = "/api/travel-notes/";
        String suffix = "/view";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return false;
        }
        String noteId = path.substring(prefix.length(), path.length() - suffix.length());
        return !noteId.isEmpty() && noteId.chars().allMatch(Character::isDigit);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
