package travel.route.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.Result;
import travel.route.feign.AttractionFeignClient;

import java.util.Collections;
import java.util.List;

/**
 * 景点服务（Feign远程调用封装）
 * 通过OpenFeign调用attraction-service获取景点数据
 */
@Service
@RequiredArgsConstructor
public class AttractionService {

    private static final Logger log = LoggerFactory.getLogger(AttractionService.class);

    private final AttractionFeignClient attractionFeignClient;

    public Attraction getById(Integer id) {
        try {
            Result<Attraction> result = attractionFeignClient.getById(id);
            return result != null && result.isSuccess() ? result.getData() : null;
        } catch (Exception e) {
            log.error("Feign调用getById失败: id={}, error={}", id, e.getMessage());
            return null;
        }
    }

    public List<Attraction> getByCityId(Integer cityId) {
        try {
            Result<List<Attraction>> result = attractionFeignClient.getByCityId(cityId);
            return result != null && result.isSuccess() ? result.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Feign调用getByCityId失败: cityId={}, error={}", cityId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Attraction> search(String keyword) {
        try {
            Result<List<Attraction>> result = attractionFeignClient.search(keyword);
            return result != null && result.isSuccess() ? result.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Feign调用search失败: keyword={}, error={}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }
}