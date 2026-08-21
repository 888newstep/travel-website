package travel.common.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AMapRouteServiceTest {

    private static final String API_KEY = "local-test-key";

    private final AtomicReference<URI> lastRequestUri = new AtomicReference<>();
    private final AtomicReference<ResponsePlan> responsePlan = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/direction/driving", this::handleRequest);
        server.start();
        respond(200, validRouteResponse(), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldBuildTwoPointRequestWithoutEmptyWaypointsAndParseResponse() {
        AMapRouteService.RouteInfo result = service(500, 4096).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(115.992817, 40.357494)));

        assertEquals(12.345, result.getDistance(), 0.0001);
        assertEquals(30.0, result.getDuration(), 0.0001);
        assertEquals(8.5, result.getCost(), 0.0001);
        assertEquals(2, result.getSteps().size());

        Map<String, String> query = queryParameters();
        assertEquals("116.390189,39.916527", query.get("origin"));
        assertEquals("115.992817,40.357494", query.get("destination"));
        assertEquals("all", query.get("extensions"));
        assertEquals("0", query.get("strategy"));
        assertEquals(API_KEY, query.get("key"));
        assertFalse(query.containsKey("waypoints"));
    }

    @Test
    void shouldIncludeOnlyIntermediatePointsForMultiPointRoute() {
        AMapRouteService.RouteInfo result = service(500, 4096).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(116.407281, 39.883355),
                point(115.992817, 40.357494)));

        assertEquals(12.345, result.getDistance(), 0.0001);
        assertEquals("116.407281,39.883355", queryParameters().get("waypoints"));
    }

    @Test
    void shouldReturnNullWhenAmapReportsQuotaFailure() {
        respond(200, "{\"status\":\"0\",\"info\":\"DAILY_QUERY_OVER_LIMIT\",\"infocode\":\"10044\"}", 0);

        AMapRouteService.RouteInfo result = service(500, 4096).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(115.992817, 40.357494)));

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenAmapReturnsNoPath() {
        respond(200, "{\"status\":\"1\",\"route\":{\"paths\":[]}}", 0);

        AMapRouteService.RouteInfo result = service(500, 4096).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(115.992817, 40.357494)));

        assertNull(result);
    }

    @Test
    void shouldReturnNullOnReadTimeout() {
        respond(200, validRouteResponse(), 300);

        AMapRouteService.RouteInfo result = service(50, 4096).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(115.992817, 40.357494)));

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenResponseExceedsLimit() {
        respond(200, "x".repeat(512), 0);

        AMapRouteService.RouteInfo result = service(500, 64).calculateMultiPointRoute(List.of(
                point(116.390189, 39.916527),
                point(115.992817, 40.357494)));

        assertNull(result);
    }

    @Test
    void shouldRejectInvalidCoordinatesBeforeCallingAmap() {
        AMapRouteService routeService = service(500, 4096);

        assertThrows(IllegalArgumentException.class, () -> routeService.calculateMultiPointRoute(List.of(
                point(181, 39.916527),
                point(115.992817, 40.357494))));
        assertNull(lastRequestUri.get());
    }

    @Test
    void shouldNotLogApiKeyWhenHttpCallFails() {
        respond(500, "upstream failure", 0);
        Logger logger = (Logger) LoggerFactory.getLogger(AMapRouteService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            AMapRouteService.RouteInfo result = service(500, 4096).calculateMultiPointRoute(List.of(
                    point(116.390189, 39.916527),
                    point(115.992817, 40.357494)));

            assertNull(result);
            assertTrue(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains(API_KEY)));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private AMapRouteService service(int readTimeoutMillis, long maxResponseBytes) {
        ExternalCallBulkheadRegistry registry = new ExternalCallBulkheadRegistry(50, 4, 1, 1);
        AMapRouteService service = new AMapRouteService(
                500,
                readTimeoutMillis,
                maxResponseBytes,
                registry);
        ReflectionTestUtils.setField(service, "apiKey", API_KEY);
        ReflectionTestUtils.setField(service, "apiUrl", baseUrl());
        return service;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v3";
    }

    private double[] point(double longitude, double latitude) {
        return new double[]{longitude, latitude};
    }

    private void respond(int status, String body, long delayMillis) {
        responsePlan.set(new ResponsePlan(status, body, delayMillis));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        lastRequestUri.set(exchange.getRequestURI());
        ResponsePlan plan = responsePlan.get();
        if (plan.delayMillis() > 0) {
            try {
                Thread.sleep(plan.delayMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = plan.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        try {
            exchange.sendResponseHeaders(plan.status(), body.length);
            exchange.getResponseBody().write(body);
        } catch (IOException ignored) {
        } finally {
            exchange.close();
        }
    }

    private Map<String, String> queryParameters() {
        String rawQuery = lastRequestUri.get().getRawQuery();
        Map<String, String> parameters = new LinkedHashMap<>();
        Arrays.stream(rawQuery.split("&")).forEach(part -> {
            String[] pair = part.split("=", 2);
            parameters.put(
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        });
        return parameters;
    }

    private String validRouteResponse() {
        return """
                {
                  "status": "1",
                  "route": {
                    "paths": [
                      {
                        "distance": "12345",
                        "duration": "1800",
                        "tolls": "8.5",
                        "steps": [
                          {"instruction": "向东行驶", "distance": "5000", "duration": "600"},
                          {"instruction": "继续直行", "distance": "7345", "duration": "1200"}
                        ]
                      }
                    ]
                  }
                }
                """;
    }

    private record ResponsePlan(int status, String body, long delayMillis) {
    }
}
