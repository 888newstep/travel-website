package travel.route.algorithm;

import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Attraction;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDayClusterTest {

    private Attraction attraction(int id, double lng, double lat) {
        Attraction a = new Attraction();
        a.setId(id);
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        return a;
    }

    /** 两个地理上远离的片区（南京/北京），各若干点 */
    private List<Attraction> twoCityAttractions() {
        List<Attraction> list = new ArrayList<>();
        // 南京片区 4 个
        list.add(attraction(1, 118.78, 32.06));
        list.add(attraction(2, 118.80, 32.08));
        list.add(attraction(3, 118.76, 32.04));
        list.add(attraction(4, 118.79, 32.05));
        // 北京片区 4 个
        list.add(attraction(5, 116.40, 39.90));
        list.add(attraction(6, 116.42, 39.92));
        list.add(attraction(7, 116.38, 39.88));
        list.add(attraction(8, 116.41, 39.91));
        return list;
    }

    @Test
    void clustersAreGeographicallyCoherent() {
        List<Attraction> attractions = twoCityAttractions();
        List<List<Attraction>> clusters = RouteDayCluster.clusterByDay(attractions, 2, 42L);

        assertEquals(2, clusters.size());
        assertEquals(4, clusters.get(0).size());
        assertEquals(4, clusters.get(1).size());

        // 同簇内任意两点距离必须远小于跨簇距离
        List<Attraction> c0 = clusters.get(0);
        List<Attraction> c1 = clusters.get(1);
        double maxIntra = 0;
        for (List<Attraction> c : List.of(c0, c1)) {
            for (int i = 0; i < c.size(); i++) {
                for (int j = i + 1; j < c.size(); j++) {
                    double dx = (c.get(i).getLongitude().doubleValue() - c.get(j).getLongitude().doubleValue()) * 111.32;
                    double dy = (c.get(i).getLatitude().doubleValue() - c.get(j).getLatitude().doubleValue()) * 110.57;
                    maxIntra = Math.max(maxIntra, Math.sqrt(dx * dx + dy * dy));
                }
            }
        }
        double interCity = Math.abs(c0.get(0).getLongitude().doubleValue() - c1.get(0).getLongitude().doubleValue()) * 111.32;
        assertTrue(maxIntra < interCity / 10, "簇内距离应远小于跨簇距离: intra=" + maxIntra);
    }

    @Test
    void sizesAreBalanced() {
        List<Attraction> attractions = twoCityAttractions();
        // 3 天分 8 个点：容量上限 ceil(8/3)=3，各簇 3/3/2
        List<List<Attraction>> clusters = RouteDayCluster.clusterByDay(attractions, 3, 42L);
        assertEquals(3, clusters.size());
        assertEquals(attractions.size(), clusters.stream().mapToInt(List::size).sum());
        assertTrue(clusters.stream().allMatch(c -> c.size() <= 3), "每簇不得超过容量上限");
        assertTrue(clusters.stream().noneMatch(List::isEmpty), "不允许空天");
    }

    @Test
    void deterministicWithSameSeed() {
        List<Attraction> attractions = twoCityAttractions();
        List<List<Attraction>> a = RouteDayCluster.clusterByDay(attractions, 2, 42L);
        List<List<Attraction>> b = RouteDayCluster.clusterByDay(attractions, 2, 42L);
        for (int d = 0; d < a.size(); d++) {
            assertEquals(a.get(d).stream().map(Attraction::getId).toList(),
                    b.get(d).stream().map(Attraction::getId).toList(),
                    "同种子必须可复现");
        }
    }

    @Test
    void fewerAttractionsThanDays() {
        List<Attraction> attractions = List.of(attraction(1, 118.0, 32.0), attraction(2, 116.4, 39.9));
        List<List<Attraction>> clusters = RouteDayCluster.clusterByDay(attractions, 5, 42L);
        assertEquals(2, clusters.size(), "景点少于天数时按景点数分簇");
        assertEquals(1, clusters.get(0).size());
        assertEquals(1, clusters.get(1).size());
    }

    @Test
    void singleDayReturnsAll() {
        List<Attraction> attractions = twoCityAttractions();
        List<List<Attraction>> clusters = RouteDayCluster.clusterByDay(attractions, 1, 42L);
        assertEquals(1, clusters.size());
        assertEquals(8, clusters.get(0).size());
    }
}
