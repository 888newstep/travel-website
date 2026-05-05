package travel.service.travel_recommendation;

import com.baidu.aip.imageclassify.AipImageClassify;
import lombok.RequiredArgsConstructor;
import travel.config.AIConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * AI服务工厂类
 */
@Component
@RequiredArgsConstructor
public class AIServiceFactory {

    private final AIConfig aiConfig;
    private AipImageClassify baiduImageClassify;

    @PostConstruct
    public void init() {
        // 初始化百度AI服务
        if (aiConfig.getBaidu() != null && aiConfig.getBaidu().getAppId() != null) {
            baiduImageClassify = new AipImageClassify(
                    aiConfig.getBaidu().getAppId(),
                    aiConfig.getBaidu().getApiKey(),
                    aiConfig.getBaidu().getSecretKey()
            );
            // 设置网络连接参数
            baiduImageClassify.setConnectionTimeoutInMillis(2000);
            baiduImageClassify.setSocketTimeoutInMillis(60000);
        }
    }

    /**
     * 获取百度图像识别服务实例
     * @return AipImageClassify实例
     */
    public AipImageClassify getBaiduImageClassify() {
        return baiduImageClassify;
    }

    /**
     * 检查OpenAI服务是否可用
     * @return 是否可用
     */
    public boolean isOpenAiAvailable() {
        return false; // 暂时返回false，因为OpenAI服务未配置
    }

    /**
     * 检查百度AI服务是否可用
     * @return 是否可用
     */
    public boolean isBaiduAiAvailable() {
        return baiduImageClassify != null;
    }
}
