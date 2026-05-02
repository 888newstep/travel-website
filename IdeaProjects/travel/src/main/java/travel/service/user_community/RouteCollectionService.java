package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.RouteCollection;

import java.util.List;

public interface RouteCollectionService extends IService<RouteCollection> {

    RouteCollection collectRoute(Integer routeId, Integer userId, Boolean isPublic, String notes);

    boolean cancelCollect(Integer routeId, Integer userId);

    List<RouteCollection> getUserCollections(Integer userId, int page, int size);

    boolean isCollected(Integer routeId, Integer userId);

    int getRouteCollectionCount(Integer routeId);

    boolean updateCollectionNotes(Integer collectionId, Integer userId, String notes);

    boolean updateCollectionPublicStatus(Integer collectionId, Integer userId, Boolean isPublic);

    List<RouteCollection> getPublicCollections(int page, int size);

    RouteCollection addCollection(RouteCollection collection);

    boolean removeCollection(Integer userId, Integer routeId);

    boolean checkCollected(Integer userId, Integer routeId);

    List<String> getCollectionCategories(Integer userId);

    List<RouteCollection> getCollectionsByCategory(Integer userId, String category, int page, int size);

    int batchRemoveCollections(List<Long> ids);

    boolean updateCollectionNote(Long id, String note);
}