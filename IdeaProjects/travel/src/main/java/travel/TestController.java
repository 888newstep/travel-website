package travel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/simple")
    public Map<String, Object> simpleTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Simple test successful");
        result.put("timestamp", LocalDateTime.now());
        result.put("status", "ok");
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "healthy");
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}
