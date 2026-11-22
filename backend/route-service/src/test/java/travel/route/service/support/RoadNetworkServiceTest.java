package travel.route.service.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadNetworkServiceTest {

    @Test
    void degradesGracefullyWhenOsmMissing(@TempDir Path tmp) {
        // 数据文件不存在 → available=false，查询返回 -1（调用方降级坐标近似）
        RoadNetworkService svc = new RoadNetworkService(
                tmp.resolve("nanjing.osm.pbf").toString(), tmp.resolve("cache").toString());
        assertFalse(svc.isAvailable());
        assertEquals(-1, svc.roadDistanceKm(32.06, 118.78, 32.08, 118.80));
    }

    @Test
    void degradesGracefullyWhenPathBlank() {
        RoadNetworkService svc = new RoadNetworkService("", "./graph-cache-test");
        assertFalse(svc.isAvailable());
        assertEquals(-1, svc.roadDistanceKm(32.06, 118.78, 32.08, 118.80));
    }
}
