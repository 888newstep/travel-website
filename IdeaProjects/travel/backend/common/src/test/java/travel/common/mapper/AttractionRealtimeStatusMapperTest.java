package travel.common.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttractionRealtimeStatusMapperTest {

    @Test
    void shouldUpdateSyncTimeByAttractionIdInsteadOfPrimaryKey() throws Exception {
        Method method = AttractionRealtimeStatusMapper.class
                .getMethod("batchUpdateSyncTime", Long[].class);
        String sql = String.join(" ", method.getAnnotation(Update.class).value());

        assertTrue(sql.contains("WHERE attraction_id IN"));
        assertFalse(sql.contains("WHERE id IN"));
    }
}
