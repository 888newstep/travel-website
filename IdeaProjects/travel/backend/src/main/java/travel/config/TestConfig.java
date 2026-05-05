package travel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 测试环境配置
 * 仅在test profile激活时生效
 */
@Configuration
@Profile("test")
public class TestConfig {
    // 测试环境特定配置 - Redis Mock 由 RedisConfig 提供
}
