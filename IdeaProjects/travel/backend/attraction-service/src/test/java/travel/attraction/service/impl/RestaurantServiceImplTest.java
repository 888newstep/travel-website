package travel.attraction.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Restaurant;
import travel.common.exception.BusinessException;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class RestaurantServiceImplTest {

    @Test
    void search_shouldGroupKeywordConditionsUnderCityFilter() {
        RestaurantServiceImpl service = spy(new RestaurantServiceImpl());
        AtomicReference<Wrapper<Restaurant>> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return List.of();
        }).when(service).list(any(Wrapper.class));

        service.search(3, "面馆");

        String sql = ((QueryWrapper<Restaurant>) captured.get()).getCustomSqlSegment();
        assertTrue(sql.contains("city_id"));
        assertTrue(sql.contains("AND (name LIKE"));
        assertTrue(sql.contains("OR feature LIKE"));
        assertTrue(sql.contains("OR cuisine_type LIKE"));
    }

    @Test
    void getByCityId_shouldPropagateDatabaseFailure() {
        RestaurantServiceImpl service = spy(new RestaurantServiceImpl());
        RuntimeException failure = new RuntimeException("database unavailable");
        doThrow(failure).when(service).list(any(Wrapper.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class, () -> service.getByCityId(3));

        assertEquals(failure, thrown);
    }

    @Test
    void getRestaurantDetail_shouldReportNotFound() {
        RestaurantServiceImpl service = spy(new RestaurantServiceImpl());
        doReturn(null).when(service).getById(7);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.getRestaurantDetail(7));

        assertEquals(22014, exception.getCode());
    }
}
