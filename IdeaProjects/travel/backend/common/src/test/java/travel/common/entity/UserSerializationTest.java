package travel.common.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import travel.common.entity.user_community.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNeverSerializePassword() throws Exception {
        User user = new User();
        user.setId(42);
        user.setUsername("traveler");
        user.setPassword("encoded-password");

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("encoded-password"));
        assertFalse(user.toString().contains("encoded-password"));
    }

    @Test
    void shouldStillAcceptPasswordForRequestBinding() throws Exception {
        User user = objectMapper.readValue(
                "{\"username\":\"traveler\",\"password\":\"raw-password\"}",
                User.class);

        assertEquals("raw-password", user.getPassword());
    }
}
