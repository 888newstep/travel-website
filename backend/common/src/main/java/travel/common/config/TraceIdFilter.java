package travel.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * traceId 全链路贯穿：
 * - 优先透传 Gateway 下发的 X-Trace-Id（仅接受安全字符，防日志注入）；
 * - 直连（本地调试/绕过网关）时生成新 traceId；
 * - 写入 MDC 供日志 pattern 输出，并回写响应头便于前端/联调对账。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = normalize(request.getHeader(TRACE_HEADER));
        if (traceId == null) {
            traceId = newTraceId();
        }
        MDC.put(MDC_TRACE_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }

    static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 仅接受 1-64 位十六进制/横线字符；缺失或非法时返回 null 由调用方重新生成，
     * 防止外部注入换行等控制字符污染日志。
     */
    static String normalize(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64 || !trimmed.matches("[0-9a-fA-F-]+")) {
            return null;
        }
        return trimmed;
    }
}
