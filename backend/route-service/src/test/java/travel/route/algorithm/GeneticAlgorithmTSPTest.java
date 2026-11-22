package travel.route.algorithm;

import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Attraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticAlgorithmTSPTest {

    private final GeneticAlgorithmTSP ga = new GeneticAlgorithmTSP();

    private Attraction attraction(int id, double lng, double lat) {
        Attraction a = new Attraction();
        a.setId(id);
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        return a;
    }

    /** 线上等距点：最优访问顺序应接近单向扫过 */
    private List<Attraction> lineAttractions(int n) {
        List<Attraction> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(attraction(i, 118.0 + i * 0.01, 32.0 + (i % 2) * 0.005));
        }
        return list;
    }

    private double tourDistance(List<Attraction> tour) {
        double d = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            double dx = (tour.get(i).getLongitude().doubleValue() - tour.get(i + 1).getLongitude().doubleValue()) * 111.32;
            double dy = (tour.get(i).getLatitude().doubleValue() - tour.get(i + 1).getLatitude().doubleValue()) * 110.57;
            d += Math.sqrt(dx * dx + dy * dy);
        }
        return d;
    }

    @Test
    void smallInputReturnedAsIs() {
        List<Attraction> two = Arrays.asList(attraction(1, 118.0, 32.0), attraction(2, 118.1, 32.1));
        assertEquals(two, ga.optimizeRoute(two, "distance"));
    }

    @Test
    void resultIsValidPermutation() {
        List<Attraction> attractions = lineAttractions(15);
        List<Attraction> result = ga.optimizeRoute(attractions, "distance");
        assertEquals(attractions.size(), result.size());
        long distinct = result.stream().map(Attraction::getId).distinct().count();
        assertEquals(attractions.size(), distinct, "每个景点必须恰好出现一次");
        assertTrue(new ArrayList<>(result).containsAll(attractions));
    }

    @Test
    void optimizedTourBeatsNaiveOrder() {
        // 交错坐标制造"朴素顺序=来回折返"的场景
        List<Attraction> attractions = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            // 偶数在东侧、奇数在西侧：朴素顺序来回跑，最优应分侧扫过
            attractions.add(attraction(i, i % 2 == 0 ? 118.10 : 118.00, 32.0 + i * 0.002));
        }
        List<Attraction> result = ga.optimizeRoute(attractions, "distance");
        assertTrue(tourDistance(result) < tourDistance(attractions),
                "优化后总距离应严格小于朴素顺序");
    }

    @Test
    void missingCoordinatesTreatedAsWorst() {
        List<Attraction> attractions = lineAttractions(6);
        attractions.get(3).setLatitude(null);
        // 不应抛异常，且仍返回全部 n 个景点
        List<Attraction> result = ga.optimizeRoute(attractions, "distance");
        assertEquals(attractions.size(), result.size());
    }

    @Test
    void largeScaleFastPathReturnsValidPermutation() {
        // 2230 组暴力基准结论：n>400 走 NN+2-opt 快速路径（GA 质量优势消失且时间爆炸）
        List<Attraction> attractions = lineAttractions(450);
        long t0 = System.currentTimeMillis();
        List<Attraction> result = ga.optimizeRoute(attractions, "distance");
        long ms = System.currentTimeMillis() - t0;
        assertEquals(attractions.size(), result.size());
        long distinct = result.stream().map(Attraction::getId).distinct().count();
        assertEquals(attractions.size(), distinct);
        assertTrue(ms < 5000, "n=450 快速路径应在秒级内，实际 " + ms + "ms");
    }
}