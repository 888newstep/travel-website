package travel.route.algorithm;

import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Attraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitnessContextTest {

    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "历史", List.of("博物馆", "古迹"),
            "自然", List.of("公园", "山"));

    private Attraction attraction(String name, String desc, double price, double lng, double lat) {
        Attraction a = new Attraction();
        a.setName(name);
        a.setDescription(desc);
        a.setTicketPrice(BigDecimal.valueOf(price));
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        return a;
    }

    @Test
    void budgetRepairDropsWorstValueFirst() {
        // 高兴趣低价(保留) vs 低兴趣天价(淘汰)
        List<Attraction> list = new ArrayList<>(List.of(
                attraction("博物馆A", "历史博物馆", 10, 118.0, 32.0),
                attraction("豪华乐园", "顶级娱乐乐园", 800, 118.1, 32.1),
                attraction("古迹B", "历史古迹", 20, 118.2, 32.2)));
        FitnessContext ctx = FitnessContext.of(list,
                Map.of("历史", 1.0, "娱乐", 0.2), KEYWORDS, BigDecimal.valueOf(100), 0);

        List<Attraction> kept = ctx.repairBudget();
        double total = kept.stream().mapToDouble(a -> a.getTicketPrice().doubleValue()).sum();
        assertTrue(total <= 100, "修复后必须在预算内");
        assertTrue(kept.stream().anyMatch(a -> a.getName().equals("博物馆A")));
        assertTrue(kept.stream().anyMatch(a -> a.getName().equals("古迹B")));
        assertTrue(kept.stream().noneMatch(a -> a.getName().equals("豪华乐园")),
                "低性价比高票价应被淘汰");
        assertTrue(ctx.getDroppedByRepair().get(0).contains("豪华乐园"));
    }

    @Test
    void freeAttractionsNeverDroppedByBudget() {
        List<Attraction> list = new ArrayList<>(List.of(
                attraction("免费公园", "自然公园", 0, 118.0, 32.0),
                attraction("收费古迹", "历史古迹", 50, 118.1, 32.1)));
        FitnessContext ctx = FitnessContext.of(list,
                Map.of("自然", 1.0), KEYWORDS, BigDecimal.valueOf(10), 0);

        List<Attraction> kept = ctx.repairBudget();
        assertTrue(kept.stream().anyMatch(a -> a.getName().equals("免费公园")),
                "免费景点不参与预算淘汰");
        assertTrue(kept.stream().noneMatch(a -> a.getName().equals("收费古迹")));
    }

    @Test
    void maxLegRepairDropsUnfixableFarAttraction() {
        // A -> B 相距约 37km 超限；B 与任何其他站的交换都救不回来（只有 3 站，C 离 A 1km）
        List<Attraction> ordered = List.of(
                attraction("A", "", 0, 118.00, 32.00),
                attraction("B", "", 0, 118.40, 32.00),
                attraction("C", "", 0, 118.01, 32.00));
        FitnessContext ctx = FitnessContext.of(ordered, Map.of(), KEYWORDS, null, 5.0);

        List<Attraction> repaired = ctx.repairMaxLeg(ordered);
        assertTrue(repaired.size() < 3, "不可修复的远站应被淘汰");
        assertTrue(repaired.stream().noneMatch(a -> a.getName().equals("B")));
        assertTrue(ctx.getDroppedByRepair().get(0).contains("B"));
    }

    @Test
    void maxLegRepairPassesWhenAllLegsFine() {
        List<Attraction> ordered = List.of(
                attraction("A", "", 0, 118.00, 32.00),
                attraction("C", "", 0, 118.01, 32.00),
                attraction("D", "", 0, 118.02, 32.00));
        FitnessContext ctx = FitnessContext.of(ordered, Map.of(), KEYWORDS, null, 5.0);
        assertEquals(ordered, ctx.repairMaxLeg(ordered));
        assertTrue(ctx.getDroppedByRepair().isEmpty());
    }

    @Test
    void noConstraintPassthrough() {
        List<Attraction> list = List.of(attraction("X", "", 100, 118.0, 32.0));
        FitnessContext ctx = FitnessContext.of(list, Map.of(), KEYWORDS, null, 0);
        assertEquals(list, ctx.repairBudget());
        assertEquals(list, ctx.repairMaxLeg(new ArrayList<>(list)));
        assertTrue(ctx.getDroppedByRepair().isEmpty());
    }

    @Test
    void gaPresetParsingFallsBackToStandard() {
        assertEquals(GaPreset.FAST, GaPreset.fromName("fast"));
        assertEquals(GaPreset.QUALITY, GaPreset.fromName(" QUALITY "));
        assertEquals(GaPreset.STANDARD, GaPreset.fromName("不存在的档位"));
        assertEquals(GaPreset.STANDARD, GaPreset.fromName(null));
    }

    @Test
    void gaPresetAutoScaleSelection() {
        assertEquals(GaPreset.FAST, GaPreset.forScale(5));
        assertEquals(GaPreset.STANDARD, GaPreset.forScale(20));
        assertEquals(GaPreset.QUALITY, GaPreset.forScale(80));
    }
}
