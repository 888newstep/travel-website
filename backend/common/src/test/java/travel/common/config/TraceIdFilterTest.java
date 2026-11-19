package travel.common.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdFilterTest {

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldGenerateTraceIdAndWriteHeaderAndMdc() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> {
            seen.set(MDC.get(TraceIdFilter.MDC_TRACE_KEY));
            assertEquals(32, seen.get().length());
        });

        assertEquals(seen.get(), response.getHeader(TraceIdFilter.TRACE_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_KEY), "请求结束后 MDC 必须清理");
    }

    @Test
    void shouldPassthroughGatewayTraceId() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routes");
        request.addHeader(TraceIdFilter.TRACE_HEADER, "abc123-def456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) ->
                seen.set(MDC.get(TraceIdFilter.MDC_TRACE_KEY)));

        assertEquals("abc123-def456", seen.get());
        assertEquals("abc123-def456", response.getHeader(TraceIdFilter.TRACE_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_KEY));
    }

    @Test
    void shouldRejectInjectionAndMalformedTraceIds() {
        assertNull(TraceIdFilter.normalize(null));
        assertNull(TraceIdFilter.normalize(""));
        assertNull(TraceIdFilter.normalize("id\ninjected"));   // 换行注入
        assertNull(TraceIdFilter.normalize("id; DROP TABLE"));  // 非法字符
        assertNull(TraceIdFilter.normalize("x".repeat(65)));    // 超长
        assertNotNull(TraceIdFilter.normalize("ABC-0123-DEF")); // 合法
        assertEquals(32, TraceIdFilter.newTraceId().length());
        assertTrue(TraceIdFilter.newTraceId().matches("[0-9a-f]{32}"));
    }
}
