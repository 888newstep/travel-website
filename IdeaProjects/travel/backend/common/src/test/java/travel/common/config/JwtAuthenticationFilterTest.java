package travel.common.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.utils.CacheUtil;
import travel.common.utils.JwtHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = "test-secret-key-with-at-least-32-characters";

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", JWT_SECRET);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        System.clearProperty("jwt.secret");
    }

    @Test
    void shouldMapAdminUserTypeToAdminRole() throws Exception {
        authenticateWithUserType(9, "ROLE_ADMIN");
    }

    @Test
    void shouldMapRegularUserTypeToUserRole() throws Exception {
        authenticateWithUserType(1, "ROLE_USER");
    }

    @Test
    void shouldNotLogBlacklistedToken() throws Exception {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        String token = JwtHelper.createToken(42L, 1);
        when(cacheUtil.exists("blacklist:token:" + token)).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(cacheUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/attractions/1/review");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertTrue(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains(token)));
            verify(filterChain).doFilter(request, response);
        } finally {
            logger.detachAppender(appender);
        }
    }

    private void authenticateWithUserType(Integer userType, String expectedRole) throws Exception {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        when(cacheUtil.exists(anyString())).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(cacheUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + JwtHelper.createToken(42L, userType));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertEquals(42L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> expectedRole.equals(authority.getAuthority())));
    }
}
