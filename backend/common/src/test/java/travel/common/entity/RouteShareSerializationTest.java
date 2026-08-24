package travel.common.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import travel.common.entity.user_community.RouteShare;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteShareSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNotSerializeSharePassword() throws Exception {
        RouteShare share = new RouteShare();
        share.setShareCode("ABC123");
        share.setPassword("secret");

        String json = objectMapper.writeValueAsString(share);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret"));
        assertFalse(share.toString().contains("secret"));
    }
}
