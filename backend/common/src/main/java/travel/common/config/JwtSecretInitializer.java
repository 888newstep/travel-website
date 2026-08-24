package travel.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtSecretInitializer {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    void initialize() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Property 'jwt.secret' must be configured before starting backend services");
        }

        System.setProperty("jwt.secret", jwtSecret);
    }
}
