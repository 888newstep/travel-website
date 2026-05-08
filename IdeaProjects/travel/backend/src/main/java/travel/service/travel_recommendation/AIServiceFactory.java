package travel.service.travel_recommendation;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.baidu.aip.ocr.AipOcr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.config.AIConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * AI服务工厂类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIServiceFactory {

    private final AIConfig aiConfig;
    private AipImageClassify baiduImageClassify;
    private AipOcr baiduOcr;

    @PostConstruct
    public void init() {
        // 初始化百度AI服务
        if (aiConfig.getBaidu() != null && aiConfig.getBaidu().getAppId() != null) {
            String appId = aiConfig.getBaidu().getAppId();
            String apiKey = aiConfig.getBaidu().getApiKey();
            String secretKey = aiConfig.getBaidu().getSecretKey();

            // 初始化图像识别服务
            baiduImageClassify = new AipImageClassify(appId, apiKey, secretKey);
            baiduImageClassify.setConnectionTimeoutInMillis(2000);
            baiduImageClassify.setSocketTimeoutInMillis(60000);

            // 初始化OCR服务
            baiduOcr = new AipOcr(appId, apiKey, secretKey);
            baiduOcr.setConnectionTimeoutInMillis(2000);
            baiduOcr.setSocketTimeoutInMillis(60000);

            log.info("百度AI服务初始化成功");
        } else {
            log.warn("百度AI配置不完整，服务未初始化");
        }
    }

    /**
     * 获取百度图像识别服务实例
     */
    public AipImageClassify getBaiduImageClassify() {
        if (baiduImageClassify == null) {
            throw new IllegalStateException("百度图像识别服务未初始化，请检查配置");
        }
        return baiduImageClassify;
    }

    /**
     * 获取百度OCR服务实例
     */
    public AipOcr getBaiduOcr() {
        if (baiduOcr == null) {
            throw new IllegalStateException("百度OCR服务未初始化，请检查配置");
        }
        return baiduOcr;
    }

    /**
     * 检查OpenAI服务是否可用
     */
    public boolean isOpenAiAvailable() {
        return aiConfig.getEnabled() && aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty();
    }

    /**
     * 检查百度AI服务是否可用
     */
    public boolean isBaiduAiAvailable() {
        return baiduImageClassify != null && baiduOcr != null;
    }
}
