package travel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {

    private String apiKey;

    private String apiUrl = "https://api.openai.com/v1";

    private String model = "gpt-3.5-turbo";

    private Double temperature = 0.7;

    private Integer maxTokens = 1000;

    private Boolean enabled = false;

    private BaiduConfig baidu;

    private QwenConfig qwen;

    @Data
    public static class BaiduConfig {
        private String appId;
        private String apiKey;
        private String secretKey;
    }

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String model = "qwen3.6-35b-a3b";
        private Boolean enabled = false;
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
    }
}
