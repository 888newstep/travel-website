package travel.route.service;

import org.junit.jupiter.api.Test;
import travel.route.dto.route.SmartRouteItem;

import static org.junit.jupiter.api.Assertions.*;

public class IntelligentRouteServiceTest {

    @Test
    public void testSmartRouteItemBuilder() {
        SmartRouteItem item = SmartRouteItem.builder()
                .routeId(1)
                .title("Test Route")
                .description("Test Description")
                .durationDays(3)
                .difficulty("medium")
                .coverImage("image.jpg")
                .viewCount(100)
                .likeCount(50)
                .attractionCount(5)
                .similarity(0.85)
                .preference("balanced")
                .season("spring")
                .theme("历史文化")
                .build();

        assertNotNull(item);
        assertEquals(1, item.getRouteId());
        assertEquals("Test Route", item.getTitle());
        assertEquals(3, item.getDurationDays());
        assertEquals(0.85, item.getSimilarity());
    }

    @Test
    public void testSmartRouteItemDefaultValues() {
        SmartRouteItem item = SmartRouteItem.builder().build();

        assertNotNull(item);
        assertNull(item.getRouteId());
        assertNull(item.getTitle());
    }
}
