package travel.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第三方调用舱壁注册表，按供应商名称隔离并发，配置项 travel.external.* 。
 */
@Slf4j
@Component
public class ExternalCallBulkheadRegistry {

    public static final String AMAP = "amap";
    public static final String BAIDU_AI = "baidu-ai";
    public static final String OPENAI = "openai";

    private static final int DEFAULT_MAX_CONCURRENT = 8;

    private final long acquireTimeoutMillis;
    private final Map<String, ExternalCallBulkhead> bulkheads = new ConcurrentHashMap<>();

    public ExternalCallBulkheadRegistry(
            @Value("${travel.external.acquire-timeout-ms:50}") long acquireTimeoutMillis,
            @Value("${travel.external.amap.max-concurrent:32}") int amapMaxConcurrent,
            @Value("${travel.external.baidu-ai.max-concurrent:8}") int baiduAiMaxConcurrent,
            @Value("${travel.external.openai.max-concurrent:8}") int openaiMaxConcurrent) {
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        register(AMAP, amapMaxConcurrent);
        register(BAIDU_AI, baiduAiMaxConcurrent);
        register(OPENAI, openaiMaxConcurrent);
    }

    private void register(String name, int maxConcurrent) {
        bulkheads.put(name, new ExternalCallBulkhead(name, maxConcurrent, acquireTimeoutMillis));
        log.info("Registered external call bulkhead: name={}, maxConcurrent={}, acquireTimeoutMs={}",
                name, maxConcurrent, acquireTimeoutMillis);
    }

    /**
     * 按供应商名称获取舱壁；未注册的供应商按默认并发 8 懒加载注册。
     */
    public ExternalCallBulkhead get(String name) {
        return bulkheads.computeIfAbsent(name,
                n -> new ExternalCallBulkhead(n, DEFAULT_MAX_CONCURRENT, acquireTimeoutMillis));
    }
}
