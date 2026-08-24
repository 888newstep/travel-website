package travel.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启 Outbox 定时投递所需的 Spring 调度能力。 */
@Configuration
@EnableScheduling
public class OutboxSchedulingConfig {
}
