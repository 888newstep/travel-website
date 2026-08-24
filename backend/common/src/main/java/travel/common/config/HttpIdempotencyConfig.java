package travel.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import travel.common.performance.PerformanceStageRecorder;
import travel.common.service.HttpIdempotencyService;

/** HTTP idempotency filter configuration. */
@Configuration
public class HttpIdempotencyConfig {

    @Bean
    public HttpIdempotencyFilter httpIdempotencyFilter(
            HttpIdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            PerformanceStageRecorder performanceStageRecorder,
            @Value("${travel.http.idempotency.enabled:true}") boolean enabled,
            @Value("${travel.http.idempotency.max-key-length:128}") int maxKeyLength,
            @Value("${travel.http.idempotency.max-request-body-bytes:1048576}") int maxRequestBodyBytes,
            @Value("${travel.http.idempotency.max-response-body-bytes:1048576}") int maxResponseBodyBytes) {
        return new HttpIdempotencyFilter(
                idempotencyService,
                objectMapper,
                enabled,
                maxKeyLength,
                maxRequestBodyBytes,
                maxResponseBodyBytes,
                performanceStageRecorder);
    }

    @Bean
    public FilterRegistrationBean<HttpIdempotencyFilter> httpIdempotencyFilterRegistration(
            HttpIdempotencyFilter filter) {
        FilterRegistrationBean<HttpIdempotencyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("httpIdempotencyFilter");
        registration.setEnabled(false);
        return registration;
    }
}
