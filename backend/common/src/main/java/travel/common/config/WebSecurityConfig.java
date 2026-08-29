package travel.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import travel.common.utils.CacheUtil;
import travel.common.performance.PerformanceStageRecorder;

/**
 * Spring Security 安全配置
 * 原则：只读公开，写入需认证
 * - 公开接口：登录注册、静态资源、Swagger、只读浏览（GET）
 * - 认证接口：所有写操作（POST/PUT/DELETE）、用户数据、收藏评论等
 * - AI 生成接口统一要求认证，避免匿名成本滥用
 */
@Configuration
@EnableWebSecurity
@Order(2)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CacheUtil cacheUtil;
    private final HttpIdempotencyFilter httpIdempotencyFilter;
    private final PerformanceStageRecorder performanceStageRecorder;

    /**
     * 仅供本机性能压测读取 Actuator/Druid 诊断数据，默认关闭，避免把运维端点暴露给外部请求。
     */
    @Value("${travel.performance.metrics-endpoints-enabled:false}")
    private boolean performanceMetricsEndpointsEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                        // ===== 完全公开（无需任何认证） =====
                        // 跨域预检请求
                        authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        // 静态资源
                        authorize.requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll();
                        // 认证相关（登录、注册、验证码）
                        authorize.requestMatchers("/users/login", "/users/register", "/users/captcha").permitAll();
                        // Swagger 文档
                        authorize.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/doc.html", "/webjars/**").permitAll();
                        // 健康检查
                        authorize.requestMatchers("/health", "/version", "/actuator/health", "/actuator/health/**").permitAll();

                        if (performanceMetricsEndpointsEnabled) {
                            // 性能脚本只从本机回环地址读取指标，生产默认不开启该分支。
                            authorize.requestMatchers(HttpMethod.GET, "/actuator/**", "/druid/*.json")
                                    .access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1')"));
                        }

                        // ===== 公开只读（GET 浏览） =====
                        authorize.requestMatchers(HttpMethod.GET, "/attractions", "/attractions/**").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/cities", "/cities/**").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/routes", "/routes/city/**", "/routes/search", "/routes/{id}").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/routes/{id}/schedule", "/routes/{id}/traffic").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/restaurants", "/restaurants/**").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/travel-notes/**").permitAll();
                        authorize.requestMatchers(HttpMethod.POST, "/travel-notes/*/view").permitAll();
                        authorize.requestMatchers(HttpMethod.GET,
                                "/route-share/validate",
                                "/route-share/info/**",
                                "/route-share/access/**",
                                "/route-share/file/access/**").permitAll();
                        authorize.requestMatchers(HttpMethod.POST, "/route-share/visit/**").permitAll();

                        // ===== 管理员接口 =====
                        authorize.requestMatchers(HttpMethod.POST, "/feedback/reply/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.PUT, "/feedback/process/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.GET, "/feedback/statistics", "/feedback/type/**")
                                .hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.POST, "/resource-file/category/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.PUT, "/resource-file/category/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.DELETE, "/resource-file/category/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.POST, "/attractions/{id}/review").authenticated();
                        authorize.requestMatchers(HttpMethod.POST, "/attractions/increment-views/**").authenticated();
                        authorize.requestMatchers(HttpMethod.POST, "/attractions/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.PUT, "/attractions/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.DELETE, "/attractions/**").hasRole("ADMIN");
                        authorize.requestMatchers(HttpMethod.POST,
                                "/realtime-status/update",
                                "/realtime-status/batch-update",
                                "/realtime-status/sync-time",
                                "/realtime-status/traffic-update",
                                "/realtime-status/traffic-batch").hasRole("ADMIN");
                        // ===== AI 生成服务 =====
                        // 统一要求登录，避免匿名调用外部模型造成费用、配额和线程资源滥用。
                        // 具体接口的用户数据访问仍必须在 Service 层执行对象所有权校验。
                        authorize.requestMatchers("/ai/**").authenticated();

                        // ===== 其余所有请求需要认证 =====
                        authorize.anyRequest().authenticated();
                });
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(httpIdempotencyFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(cacheUtil, performanceStageRecorder);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
