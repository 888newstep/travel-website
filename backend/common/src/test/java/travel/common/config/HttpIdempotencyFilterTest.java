package travel.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.service.HttpIdempotencyService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpIdempotencyFilterTest {

    @Mock
    private HttpIdempotencyService idempotencyService;

    private HttpIdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HttpIdempotencyFilter(
                idempotencyService,
                new ObjectMapper(),
                true,
                128,
                1024,
                1024);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldKeepRequestsWithoutIdempotencyKeyCompatible() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("normal");

        filter.doFilter(request, response, chain);

        assertEquals("normal", response.getContentAsString());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void shouldExecuteFirstRequestAndStoreSuccessfulResponse() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{\"name\":\"one\"}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpIdempotencyService.RequestMetadata metadata =
                new HttpIdempotencyService.RequestMetadata("42", "POST", "/items", "fingerprint");
        HttpIdempotencyService.ClaimResult claim =
                new HttpIdempotencyService.ClaimResult(
                        HttpIdempotencyService.ClaimStatus.CLAIMED,
                        "redis-key",
                        "token",
                        metadata,
                        null);
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(claim);
        when(idempotencyService.complete(any(), any())).thenReturn(true);

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            jakarta.servlet.http.HttpServletResponse httpResponse =
                    (jakarta.servlet.http.HttpServletResponse) filteredResponse;
            assertEquals("{\"name\":\"one\"}",
                    new String(filteredRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            httpResponse.setStatus(201);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("created");
        });

        assertEquals(201, response.getStatus());
        assertEquals("created", response.getContentAsString());
        ArgumentCaptor<HttpIdempotencyService.StoredResponse> responseCaptor =
                ArgumentCaptor.forClass(HttpIdempotencyService.StoredResponse.class);
        verify(idempotencyService).complete(any(), responseCaptor.capture());
        assertEquals(201, responseCaptor.getValue().status());
        assertEquals("created", new String(
                Base64.getDecoder().decode(responseCaptor.getValue().bodyBase64()),
                StandardCharsets.UTF_8));
    }

    @Test
    void shouldReplayCompletedResponseWithoutExecutingChain() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenAnswer(invocation -> {
            HttpIdempotencyService.RequestMetadata metadata = invocation.getArgument(2);
            HttpIdempotencyService.StoredRecord record = new HttpIdempotencyService.StoredRecord(
                    HttpIdempotencyService.COMPLETED,
                    metadata.scope(),
                    metadata.method(),
                    metadata.path(),
                    metadata.fingerprint(),
                    new HttpIdempotencyService.StoredResponse(
                            201,
                            "application/json",
                            Base64.getEncoder().encodeToString("created".getBytes(StandardCharsets.UTF_8))));
            return new HttpIdempotencyService.ClaimResult(
                    HttpIdempotencyService.ClaimStatus.COMPLETED,
                    "redis-key",
                    null,
                    metadata,
                    record);
        });

        filter.doFilter(request, response, (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("must-not-run"));

        assertEquals(201, response.getStatus());
        assertEquals("created", response.getContentAsString());
        assertEquals("true", response.getHeader(HttpIdempotencyFilter.REPLAYED_HEADER));
        verify(idempotencyService, never()).complete(any(), any());
        verify(idempotencyService, never()).release(any());
    }

    @Test
    void shouldRejectSameKeyForDifferentRequest() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "different");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpIdempotencyService.StoredRecord record = new HttpIdempotencyService.StoredRecord(
                HttpIdempotencyService.COMPLETED,
                "42",
                "POST",
                "/items",
                "other-fingerprint",
                new HttpIdempotencyService.StoredResponse(201, "application/json", ""));
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(
                new HttpIdempotencyService.ClaimResult(
                        HttpIdempotencyService.ClaimStatus.COMPLETED,
                        "redis-key",
                        null,
                        null,
                        record));

        filter.doFilter(request, response, (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("must-not-run"));

        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("different request"));
    }

    @Test
    void shouldRejectRequestWhileKeyIsProcessing() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(
                new HttpIdempotencyService.ClaimResult(
                        HttpIdempotencyService.ClaimStatus.IN_PROGRESS,
                        "redis-key",
                        null,
                        null,
                        null));

        filter.doFilter(request, response, (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("must-not-run"));

        assertEquals(409, response.getStatus());
        verify(idempotencyService, never()).complete(any(), any());
    }

    @Test
    void shouldFailClosedWhenIdempotencyStorageIsUnavailable() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-storage-error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(idempotencyService.tryClaim(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            throw new AssertionError("business chain must not execute while idempotency storage is unavailable");
        });

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("temporarily unavailable"));
        verify(idempotencyService, never()).complete(any(), any());
        verify(idempotencyService, never()).release(any());
    }

    @Test
    void shouldReleaseClaimAfterServerError() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpIdempotencyService.ClaimResult claim = new HttpIdempotencyService.ClaimResult(
                HttpIdempotencyService.ClaimStatus.CLAIMED,
                "redis-key",
                "token",
                new HttpIdempotencyService.RequestMetadata("42", "POST", "/items", "fingerprint"),
                null);
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(claim);

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            jakarta.servlet.http.HttpServletResponse httpResponse =
                    (jakarta.servlet.http.HttpServletResponse) filteredResponse;
            httpResponse.setStatus(500);
            httpResponse.getWriter().write("failed");
        });

        assertEquals(500, response.getStatus());
        verify(idempotencyService).release(claim);
        verify(idempotencyService, never()).complete(any(), any());
    }

    @Test
    void shouldKeepClaimWhenResponsePersistenceFailsAfterBusinessSuccess() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-6");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpIdempotencyService.ClaimResult claim = claimed("request-6");
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(claim);
        when(idempotencyService.complete(any(), any())).thenReturn(false);

        filter.doFilter(request, response, (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("created"));

        assertEquals(200, response.getStatus());
        verify(idempotencyService).complete(any(), any());
        verify(idempotencyService, never()).release(any());
    }

    @Test
    void shouldKeepClaimForRedirectWithoutReplayableLocationHeader() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpIdempotencyService.ClaimResult claim = claimed("request-7");
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenReturn(claim);

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            jakarta.servlet.http.HttpServletResponse httpResponse =
                    (jakarta.servlet.http.HttpServletResponse) filteredResponse;
            httpResponse.setStatus(302);
            httpResponse.setHeader("Location", "/items/1");
        });

        assertEquals(302, response.getStatus());
        verify(idempotencyService, never()).release(claim);
        verify(idempotencyService, never()).complete(any(), any());
    }

    @Test
    void shouldRejectOversizedStoredResponseInsteadOfReplayingIt() throws Exception {
        MockHttpServletRequest request = request("POST", "/items", "{}");
        request.addHeader(HttpIdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "request-8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String oversizedBody = "x".repeat(1025);
        when(idempotencyService.tryClaim(anyString(), anyString(), any())).thenAnswer(invocation -> {
            HttpIdempotencyService.RequestMetadata metadata = invocation.getArgument(2);
            HttpIdempotencyService.StoredRecord record = new HttpIdempotencyService.StoredRecord(
                    HttpIdempotencyService.COMPLETED,
                    metadata.scope(),
                    metadata.method(),
                    metadata.path(),
                    metadata.fingerprint(),
                    new HttpIdempotencyService.StoredResponse(
                            200,
                            "application/json",
                            Base64.getEncoder().encodeToString(oversizedBody.getBytes(StandardCharsets.UTF_8))));
            return new HttpIdempotencyService.ClaimResult(
                    HttpIdempotencyService.ClaimStatus.COMPLETED,
                    "redis-key",
                    null,
                    metadata,
                    record);
        });

        filter.doFilter(request, response, (filteredRequest, filteredResponse) ->
                filteredResponse.getWriter().write("must-not-run"));

        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("too large"));
    }

    private HttpIdempotencyService.ClaimResult claimed(String idempotencyKey) {
        return new HttpIdempotencyService.ClaimResult(
                HttpIdempotencyService.ClaimStatus.CLAIMED,
                "redis-key-" + idempotencyKey,
                "token-" + idempotencyKey,
                new HttpIdempotencyService.RequestMetadata("42", "POST", "/items", "fingerprint"),
                null);
    }

    private MockHttpServletRequest request(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
