package travel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import travel.utils.CacheUtil;

@Configuration
@EnableWebSecurity
@Order(2)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CacheUtil cacheUtil;

    /**
     * 配置安全过滤器链
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 禁用CSRF保护，因为我们使用JWT进行认证
        http.csrf(AbstractHttpConfigurer::disable)
                // 配置请求授权规则
                .authorizeHttpRequests(authorize -> authorize
                        // 允许所有OPTIONS请求（跨域预检）
                        .requestMatchers("OPTIONS", "/**").permitAll()
                        // 允许静态资源访问
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                        // 允许登录和注册接口访问
                        .requestMatchers("/api/users/login", "/api/users/register", "/api/users/captcha").permitAll()
                        // 允许Swagger文档访问
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/doc.html", "/webjars/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                );
                // 配置JWT认证过滤器
                http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(cacheUtil);
    }

    /**
     * 密码编码器
     * @return PasswordEncoder
     */
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
