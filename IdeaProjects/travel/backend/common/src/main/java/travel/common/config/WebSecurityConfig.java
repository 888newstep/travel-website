package travel.common.config;

import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import travel.common.utils.CacheUtil;

/**
 * Spring Security 安全配置
 * 原则：只读公开，写入需认证
 * - 公开接口：登录注册、静态资源、Swagger、只读浏览（GET）
 * - 认证接口：所有写操作（POST/PUT/DELETE）、用户数据、收藏评论等
 * - AI 对话接口公开（前端核心功能）
 */
@Configuration
@EnableWebSecurity
@Order(2)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CacheUtil cacheUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // ===== 完全公开（无需任何认证） =====
                        // 跨域预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 静态资源
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll()
                        // 认证相关（登录、注册、验证码）
                        .requestMatchers("/users/login", "/users/register", "/users/captcha").permitAll()
                        // Swagger 文档
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/doc.html", "/webjars/**").permitAll()
                        // 健康检查
                        .requestMatchers("/health", "/version").permitAll()

                        // ===== 公开只读（GET 浏览） =====
                        .requestMatchers(HttpMethod.GET, "/attractions", "/attractions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cities", "/cities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/routes", "/routes/city/**", "/routes/search", "/routes/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/restaurants", "/restaurants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/travel-notes/**").permitAll()

                        // ===== AI 服务公开（前端核心交互入口） =====
                        .requestMatchers("/ai/**").permitAll()

                        // ===== 其余所有请求需要认证 =====
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(cacheUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}