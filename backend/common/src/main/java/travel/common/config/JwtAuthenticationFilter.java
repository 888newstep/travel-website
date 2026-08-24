package travel.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import travel.common.utils.CacheUtil;
import travel.common.utils.JwtHelper;
import travel.common.performance.PerformanceStageRecorder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CacheUtil cacheUtil;
    private final PerformanceStageRecorder performanceStageRecorder;

    public JwtAuthenticationFilter(CacheUtil cacheUtil) {
        this(cacheUtil, PerformanceStageRecorder.disabled());
    }

    public JwtAuthenticationFilter(
            CacheUtil cacheUtil,
            PerformanceStageRecorder performanceStageRecorder) {
        this.cacheUtil = cacheUtil;
        this.performanceStageRecorder = performanceStageRecorder;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        long startedAtNanos = performanceStageRecorder.start();
        String outcome = "anonymous";
        try {
            String token = getTokenFromRequest(request);

            if (token != null && !JwtHelper.isExpiration(token)) {
                String blacklistKey = "blacklist:token:" + token;
                if (cacheUtil.exists(blacklistKey)) {
                    outcome = "blacklisted";
                    log.warn("检测到已登出的访问令牌: path={}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = JwtHelper.getUserId(token);

                if (userId != null) {
                    Integer userType = JwtHelper.getUserType(token);
                    String role = Integer.valueOf(9).equals(userType) ? "ROLE_ADMIN" : "ROLE_USER";
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority(role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    outcome = "authenticated";
                }
            } else if (token != null) {
                outcome = "invalid-or-expired";
            }
        } catch (Exception e) {
            outcome = "error";
            log.error("JWT authentication failed: {}", e.getMessage());
        } finally {
            performanceStageRecorder.record("service.jwt", startedAtNanos, outcome);
        }

        filterChain.doFilter(request, response);
    }
}
