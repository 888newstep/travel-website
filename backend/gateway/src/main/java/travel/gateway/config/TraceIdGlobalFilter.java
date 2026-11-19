package travel.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关边界统一生成 traceId：不信任外部传入的 X-Trace-Id（防伪造与日志注入），
 * 下游业务服务透传同一值，响应头回传供客户端与联调对账。
 * WebFlux 无 ThreadLocal 语义，MDC 由下游业务服务的 TraceIdFilter 负责。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set(TRACE_HEADER, traceId);
        ServerWebExchange mutated = exchange.mutate()
                .request(request -> request.headers(h -> h.set(TRACE_HEADER, traceId)))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
