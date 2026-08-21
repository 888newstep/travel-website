package travel.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import travel.common.service.HttpIdempotencyService;
import travel.common.utils.Result;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

/** Optional HTTP idempotency for authenticated write requests. */
@Slf4j
public class HttpIdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private final HttpIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxKeyLength;
    private final int maxRequestBodyBytes;
    private final int maxResponseBodyBytes;

    public HttpIdempotencyFilter(
            HttpIdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            boolean enabled,
            int maxKeyLength,
            int maxRequestBodyBytes,
            int maxResponseBodyBytes) {
        if (maxKeyLength <= 0 || maxRequestBodyBytes < 0 || maxResponseBodyBytes < 0) {
            throw new IllegalArgumentException("HTTP idempotency limits must be non-negative");
        }
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxKeyLength = maxKeyLength;
        this.maxRequestBodyBytes = maxRequestBodyBytes;
        this.maxResponseBodyBytes = maxResponseBodyBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || isAsyncDispatch(request) || request.getDispatcherType() == DispatcherType.ERROR) {
            return true;
        }
        String method = request.getMethod();
        if (!("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method))) {
            return true;
        }
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null) {
            filterChain.doFilter(request, response);
            return;
        }
        idempotencyKey = idempotencyKey.trim();
        if (!isValidKey(idempotencyKey)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Idempotency-Key must contain 1-" + maxKeyLength + " printable characters");
            return;
        }

        long declaredLength = request.getContentLengthLong();
        if (declaredLength > maxRequestBodyBytes) {
            writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body is too large for idempotency processing");
            return;
        }

        byte[] requestBody = readBody(request);
        if (requestBody == null) {
            writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body is too large for idempotency processing");
            return;
        }
        CachedBodyRequest cachedRequest = new CachedBodyRequest(request, requestBody);
        String scope = String.valueOf(authentication.getPrincipal());
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = requestPath(request);
        String fingerprint = fingerprint(method, path, request.getContentType(), requestBody);
        HttpIdempotencyService.RequestMetadata metadata =
                new HttpIdempotencyService.RequestMetadata(scope, method, path, fingerprint);

        HttpIdempotencyService.ClaimResult claim;
        try {
            claim = idempotencyService.tryClaim(scope, idempotencyKey, metadata);
        } catch (RuntimeException e) {
            log.error("HTTP idempotency storage unavailable for {} {}", method, path, e);
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Idempotency service is temporarily unavailable");
            return;
        }

        if (claim.status() == HttpIdempotencyService.ClaimStatus.COMPLETED) {
            if (!matches(metadata, claim.existing())) {
                writeConflict(response, "Idempotency-Key has already been used with a different request");
                return;
            }
            replay(response, claim.existing());
            return;
        }
        if (claim.status() == HttpIdempotencyService.ClaimStatus.IN_PROGRESS) {
            writeConflict(response, "A request with this Idempotency-Key is still being processed");
            return;
        }

        CapturedBodyResponse capturedResponse = new CapturedBodyResponse(response, maxResponseBodyBytes);
        try {
            filterChain.doFilter(cachedRequest, capturedResponse);
            if (request.isAsyncStarted()) {
                safeRelease(claim);
            } else if (isCacheable(capturedResponse)) {
                HttpIdempotencyService.StoredResponse storedResponse =
                        new HttpIdempotencyService.StoredResponse(
                                capturedResponse.getStatus(),
                                capturedResponse.getContentType(),
                                Base64.getEncoder().encodeToString(capturedResponse.body()));
                safeComplete(claim, storedResponse);
            } else {
                safeRelease(claim);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            safeRelease(claim);
            throw e;
        } catch (Error e) {
            safeRelease(claim);
            throw e;
        } finally {
            capturedResponse.copyBodyToResponse();
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean isValidKey(String key) {
        return !key.isEmpty()
                && key.length() <= maxKeyLength
                && key.chars().noneMatch(Character::isISOControl);
    }

    private byte[] readBody(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                Math.min(maxRequestBodyBytes, 8192));
        try (InputStream input = request.getInputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (total > maxRequestBodyBytes - read) {
                    return null;
                }
                output.write(buffer, 0, read);
                total += read;
            }
        }
        return output.toByteArray();
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String query = request.getQueryString();
        return query == null || query.isEmpty() ? path : path + "?" + query;
    }

    private String fingerprint(String method, String path, String contentType, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            if (contentType != null) {
                digest.update(contentType.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) '\n');
            byte[] result = digest.digest(body);
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte item : result) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean matches(
            HttpIdempotencyService.RequestMetadata metadata,
            HttpIdempotencyService.StoredRecord existing) {
        return existing != null
                && metadata.scope().equals(existing.scope())
                && metadata.method().equals(existing.method())
                && metadata.path().equals(existing.path())
                && metadata.fingerprint().equals(existing.fingerprint());
    }

    private boolean isCacheable(CapturedBodyResponse response) {
        int status = response.getStatus();
        if (status < 200 || status >= 500 || response.overflowed()) {
            return false;
        }
        String contentType = response.getContentType();
        return contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith("text/event-stream");
    }

    private void safeComplete(
            HttpIdempotencyService.ClaimResult claim,
            HttpIdempotencyService.StoredResponse response) {
        try {
            if (!idempotencyService.complete(claim, response)) {
                log.warn("HTTP idempotency claim expired before completion: key={}", claim.key());
            }
        } catch (RuntimeException e) {
            log.error("Failed to complete HTTP idempotency record: key={}", claim.key(), e);
            safeRelease(claim);
        }
    }

    private void safeRelease(HttpIdempotencyService.ClaimResult claim) {
        try {
            idempotencyService.release(claim);
        } catch (RuntimeException e) {
            log.error("Failed to release HTTP idempotency record: key={}", claim.key(), e);
        }
    }

    private void writeConflict(HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpServletResponse.SC_CONFLICT, message);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            objectMapper.writeValue(writer, Result.error(status, message));
        }
    }

    private void replay(
            HttpServletResponse response,
            HttpIdempotencyService.StoredRecord record) throws IOException {
        if (record == null || record.response() == null) {
            writeError(response, HttpServletResponse.SC_CONFLICT,
                    "The stored idempotency response is unavailable");
            return;
        }
        byte[] body;
        try {
            body = Base64.getDecoder().decode(record.response().bodyBase64());
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_CONFLICT,
                    "The stored idempotency response is invalid");
            return;
        }
        response.resetBuffer();
        response.setStatus(record.response().status());
        if (record.response().contentType() != null) {
            response.setContentType(record.response().contentType());
        }
        response.setHeader(REPLAYED_HEADER, "true");
        response.setContentLength(body.length);
        try (OutputStream output = response.getOutputStream()) {
            output.write(body);
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class CapturedBodyResponse extends HttpServletResponseWrapper {

        private final int maxBytes;
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private ServletOutputStream outputStream;
        private PrintWriter writer;
        private boolean overflowed;

        private CapturedBodyResponse(HttpServletResponse response, int maxBytes) {
            super(response);
            this.maxBytes = maxBytes;
        }

        private byte[] body() {
            if (writer != null) {
                writer.flush();
            }
            return body.toByteArray();
        }

        private boolean overflowed() {
            return overflowed;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called");
            }
            if (outputStream == null) {
                ServletOutputStream delegate = ((HttpServletResponse) getResponse()).getOutputStream();
                outputStream = new ServletOutputStream() {
                @Override
                public void write(int value) {
                    writeToDelegate(delegate, new byte[]{(byte) value}, 0, 1);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) {
                    writeToDelegate(delegate, bytes, offset, length);
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    delegate.setWriteListener(writeListener);
                }
                };
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called");
            }
            if (writer == null) {
                Charset charset = getCharacterEncoding() == null
                        ? StandardCharsets.UTF_8
                        : Charset.forName(getCharacterEncoding());
                writer = new PrintWriter(new OutputStreamWriter(new CapturingOutputStream(), charset), true);
            }
            return writer;
        }

        private void writeBytes(byte[] bytes, int offset, int length) {
            if (length <= 0) {
                return;
            }
            int remaining = maxBytes - body.size();
            if (length > remaining) {
                overflowed = true;
                if (remaining > 0) {
                    body.write(bytes, offset, remaining);
                }
                return;
            }
            body.write(bytes, offset, length);
        }

        private void writeToDelegate(
                ServletOutputStream delegate,
                byte[] bytes,
                int offset,
                int length) {
            try {
                delegate.write(bytes, offset, length);
                writeBytes(bytes, offset, length);
            } catch (IOException e) {
                throw new ResponseWriteException(e);
            }
        }

        private final class CapturingOutputStream extends OutputStream {
            @Override
            public void write(int value) {
                writeToDelegateUnchecked(new byte[]{(byte) value}, 0, 1);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {
                writeToDelegateUnchecked(bytes, offset, length);
            }

            private void writeToDelegateUnchecked(byte[] bytes, int offset, int length) {
                try {
                    ((HttpServletResponse) getResponse()).getOutputStream().write(bytes, offset, length);
                    writeBytes(bytes, offset, length);
                } catch (IOException e) {
                    throw new ResponseWriteException(e);
                }
            }
        }

        private void copyBodyToResponse() throws IOException {
            body();
            if (outputStream != null) {
                outputStream.flush();
            }
        }
    }

    private static final class ResponseWriteException extends RuntimeException {

        private ResponseWriteException(IOException cause) {
            super(cause);
        }
    }
}
