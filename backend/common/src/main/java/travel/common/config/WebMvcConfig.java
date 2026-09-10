package travel.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 业务服务跨域配置。生产部署通常只经 Gateway 访问，但业务服务仍保留同等白名单，
 * 防止内部端口误暴露后绕过 Gateway 的 CORS 限制。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public WebMvcConfig(@Value("${travel.security.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}}") String allowedOrigins) {
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key", "X-Requested-With")
                .exposedHeaders("Idempotency-Replayed", "X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }

    static String[] parseAllowedOrigins(String configured) {
        String[] origins = Arrays.stream(configured == null ? new String[0] : configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toArray(String[]::new);
        if (origins.length == 0 || Arrays.stream(origins).anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("CORS allowed origins must be an explicit non-empty whitelist");
        }
        return origins;
    }
}
