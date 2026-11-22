package travel.route.service.support;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.config.CHProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 街道级真路网服务（热点城市包 P2 第一块砖）。
 *
 * <p>设计（与"给不存在的输入写代码"划清界限）：
 * <ul>
 *   <li>OSM PBF 文件存在（travel.roadnet.osm-path）→ 启动时导入 + CH 预处理（ car 快速查询），
 *       roadDistanceKm 返回真实路网最短距离，毫秒级</li>
 *   <li>文件缺失 → available=false，roadDistanceKm 返回 -1，调用方优雅降级到坐标近似
 *       （分层规划的粗排允许近似，这正是度量误差实验 P90 结论的落地位置）</li>
 *   <li>城市包语义：一个实例对应一个城市的 PBF；多城扩展开箱即换（数据包即配置）</li>
 * </ul>
 */
@Service
public class RoadNetworkService {

    private static final Logger log = LoggerFactory.getLogger(RoadNetworkService.class);

    private final GraphHopper hopper;
    private final boolean available;

    public RoadNetworkService(
            @Value("${travel.roadnet.osm-path:}") String osmPath,
            @Value("${travel.roadnet.graph-cache:./graph-cache}") String graphCache) {
        GraphHopper h = null;
        boolean ok = false;
        if (osmPath != null && !osmPath.isBlank() && Files.exists(Path.of(osmPath))) {
            try {
                h = new GraphHopper();
                h.setOSMFile(osmPath);
                h.setGraphHopperLocation(graphCache);
                                // GH 9.x：Profile 只有 name/weighting/customModel，vehicle 由编码器自动匹配
                h.setProfiles(new Profile("car").setWeighting("fastest"));
                h.getCHPreparationHandler()
                        .setCHProfiles(new CHProfile("car"));
                h.importOrLoad();
                ok = true;
                log.info("路网服务就绪: osm={}, 节点数={}", osmPath, h.getBaseGraph().getNodes());
            } catch (Exception e) {
                log.error("路网加载失败，降级坐标近似: {}", e.getMessage(), e);
                if (h != null) h.close();
                h = null;
            }
        } else {
            log.info("未配置路网数据(travel.roadnet.osm-path={})，路网服务降级，走坐标近似", osmPath);
        }
        this.hopper = h;
        this.available = ok;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 真实路网驾车距离（km）。
     *
     * @return 距离；路网不可用或查询失败返回 -1（调用方降级坐标近似）
     */
    public double roadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (!available) {
            return -1;
        }
        try {
            GHRequest req = new GHRequest(lat1, lon1, lat2, lon2)
                    .setProfile("car")
                    .setLocale("zh_CN");
            GHResponse rsp = hopper.route(req);
            if (rsp.hasErrors()) {
                log.debug("路网查询失败(可能超出图范围): {}", rsp.getErrors());
                return -1;
            }
            return rsp.getBest().getDistance() / 1000.0;
        } catch (Exception e) {
            log.warn("路网查询异常: {}", e.getMessage());
            return -1;
        }
    }

    @PreDestroy
    void close() {
        if (hopper != null) {
            hopper.close();
        }
    }
}
