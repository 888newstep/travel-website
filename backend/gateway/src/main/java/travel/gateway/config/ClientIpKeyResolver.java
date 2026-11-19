package travel.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 限流 key：以客户端直连 IP 为维度。
 * Gateway 是流量边界，remoteAddress 即真实来源；不信任可伪造的 X-Forwarded-For。
 * 认证类接口（登录/注册/验证码）通过路由级独立的 redis-rate-limiter 参数实现更严阈值，
 * 与普通接口共享同一 resolver（不同限流参数在 Redis 中天然是不同的桶）。
 */
@Component
public class ClientIpKeyResolver implements KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress() == null ? null :
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
