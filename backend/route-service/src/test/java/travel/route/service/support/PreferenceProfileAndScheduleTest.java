package travel.route.service.support;

import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Attraction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenceProfileAndScheduleTest {

    private final PreferenceProfile profile = new PreferenceProfile();
    private final RouteScheduleValidator validator = new RouteScheduleValidator();

    private Attraction attraction(String name, String hours, double lng, double lat) {
        Attraction a = new Attraction();
        a.setName(name);
        a.setOpeningHours(hours);
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        return a;
    }

    // ---------------- PreferenceProfile ----------------

    @Test
    void preferenceKeywordsMapToMaxLeg() {
        assertEquals(15.0, profile.resolveMaxLegKm("亲子游"));
        assertEquals(15.0, profile.resolveMaxLegKm("带娃出行"));
        assertEquals(20.0, profile.resolveMaxLegKm("轻松休闲"));
        assertEquals(0, profile.resolveMaxLegKm("特种兵式暴走"));
        assertEquals(0, profile.resolveMaxLegKm("无偏好"));
        assertEquals(0, profile.resolveMaxLegKm(null));
    }

    @Test
    void conflictingPreferencesTakeStrictest() {
        // 亲子(15) 与 慢节奏(20) 同现 → 取更严格的 15
        assertEquals(15.0, profile.resolveMaxLegKm("亲子且轻松"));
    }

    @Test
    void interestWeightsBoostMentionedDimension() {
        var w = profile.resolveInterestWeights("喜欢历史");
        assertEquals(1.0, w.get("历史文化"));
        assertEquals(0.3, w.get("自然风光"));
    }

    // ---------------- RouteScheduleValidator ----------------

    @Test
    void passesWhenAllOpenAllDay() {
        List<Attraction> order = List.of(
                attraction("A", null, 118.0, 32.0),
                attraction("B", "全天开放", 118.1, 32.1));
        var result = validator.validateAndFix(order);
        assertEquals(List.of("A", "B"), result.orderedNames());
        assertTrue(result.unsatisfiable().isEmpty());
        assertEquals(0, result.swaps());
    }

    @Test
    void swapsLateArrivalWhenSwapFixes() {
        // A(近,无时间窗) B(远, 07:00-09:10 早关门): 顺序 A,B 会让 B 迟到 3 分钟；
        // 交换后 B 首站 9:00 准点到达（早到等待合法），A 无窗口 → 交换完全修复
        Attraction a = attraction("近站A", null, 118.00, 32.0);
        Attraction b = attraction("早关门B", "07:00-09:10", 118.10, 32.0);
        Attraction c = attraction("普通C", null, 118.02, 32.0);
        var result = validator.validateAndFix(new java.util.ArrayList<>(List.of(a, b, c)));
        assertTrue(result.unsatisfiable().isEmpty(),
                "交换应完全修复: " + result.unsatisfiable());
        assertEquals(1, result.swaps());
        assertEquals(List.of("早关门B", "近站A", "普通C"), result.orderedNames());
    }

    @Test
    void unfixableWindowDegradesHonestly() {
        // 唯一可达窗口是 07:00-08:00，但出发时间 09:00 永远迟到且无交换对象
        Attraction a = attraction("早市", "07:00-08:00", 118.0, 32.0);
        var result = validator.validateAndFix(new java.util.ArrayList<>(List.of(a)));
        assertEquals(1, result.unsatisfiable().size());
        assertTrue(result.unsatisfiable().get(0).contains("早市"));
    }

    @Test
    void malformedHoursTreatedAsNoWindow() {
        Attraction a = attraction("X", "营业时间请咨询景区", 118.0, 32.0);
        var result = validator.validateAndFix(List.of(a));
        assertEquals(List.of("X"), result.orderedNames());
        assertTrue(result.unsatisfiable().isEmpty());
    }
}
