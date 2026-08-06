package travel.attraction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.vo.CursorPageResult;

import java.util.List;
import java.util.Map;

public interface AttractionService extends IService<Attraction> {

    Attraction getById(Integer id);

    List<Attraction> getByCityId(Integer cityId);

    List<Attraction> getByCityIdAndType(Integer cityId, String type);

    Attraction getByIdUsingMapper(Integer attractionId);

    List<Attraction> getEnableAndSyncOpenAttractions();

    List<Attraction> getTopRated(Integer cityId, int limit);

    List<Map<String, Object>> search(Integer cityId, String keyword);

    List<Attraction> getRecommendations(Integer cityId, int limit);

    List<Attraction> search(String keyword);

    boolean removeById(Integer id);

    CursorPageResult<Attraction> getByCursor(Integer cityId, String cursor, int size);

    CursorPageResult<Attraction> getAllByCursor(String cursor, int size);

    Map<String, Object> comparePagination(Integer cityId, int page, int size);
}

