package travel.route.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI 路径挂载日配额拦截器；覆盖 7 个会触发外部模型调用的端点。
 */
@Configuration
public class AiWebMvcConfig implements WebMvcConfigurer {
    private final AiQuotaInterceptor aiQuotaInterceptor;
    private final boolean quotaEnabled;

    public AiWebMvcConfig(AiQuotaInterceptor aiQuotaInterceptor,
                          @Value("${travel.ai.quota-enabled:true}") boolean quotaEnabled) {
        this.aiQuotaInterceptor = aiQuotaInterceptor;
        this.quotaEnabled = quotaEnabled;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (quotaEnabled) {
            registry.addInterceptor(aiQuotaInterceptor)
                    .addPathPatterns("/ai/**");
        }
    }
}
