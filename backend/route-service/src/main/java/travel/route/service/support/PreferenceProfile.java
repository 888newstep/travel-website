package travel.route.service.support;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 偏好解析器：自由偏好文本 → 约束/权重（配置表驱动，新增偏好只加一行规则）。
 *
 * <p>替换 RouteOptimizationServiceImpl 中的硬编码 if 映射。
 * 规则语义：preference 文本包含任一关键词即命中；多条规则可叠加，后命中覆盖 maxLegKm。
 */
@Component
public class PreferenceProfile {

    /** 单条偏好规则：关键词组 → 相邻段距离上限（km） */
    public record Rule(List<String> keywords, double maxLegKm, String label) {
        boolean matches(String preference) {
            return keywords.stream().anyMatch(preference::contains);
        }
    }

    /** 规则表（按业务语义可继续扩展：轮椅/摄影/夜游等） */
    private static final List<Rule> LEG_RULES = List.of(
            new Rule(List.of("亲子", "带娃", "儿童"), 15.0, "亲子游"),
            new Rule(List.of("慢", "休闲", "轻松", "老年"), 20.0, "慢节奏"),
            new Rule(List.of("特种兵", "暴走"), 0, "特种兵（不限相邻距离）"));

    /** 兴趣关键词权重（进 FitnessContext 兴趣分计算） */
    /** 与 RouteOptimizationServiceImpl 原词表对齐（宽泛词保证描述匹配），亲子/夜市等为新增维度 */
    public static final Map<String, List<String>> INTEREST_KEYWORDS = Map.of(
            "历史文化", List.of("博物馆", "古迹", "文化", "历史", "遗址", "故居"),
            "自然风光", List.of("公园", "山水", "自然", "风景", "山", "湖", "森林"),
            "亲子", List.of("乐园", "动物园", "水族", "儿童"),
            "美食", List.of("美食", "餐厅", "小吃", "饮食", "夜市"),
            "购物", List.of("商场", "购物", "步行街", "商圈"),
            "娱乐", List.of("乐园", "娱乐", "休闲", "活动"));

    /**
     * 解析相邻段距离上限；0 = 不限。
     * 多条规则命中取最严格（最小非零）值——约束冲突时向保守侧收敛。
     */
    public double resolveMaxLegKm(String preference) {
        String pref = preference == null ? "" : preference;
        double resolved = 0;
        String label = "";
        for (Rule rule : LEG_RULES) {
            if (rule.matches(pref) && rule.maxLegKm > 0) {
                if (resolved == 0 || rule.maxLegKm < resolved) {
                    resolved = rule.maxLegKm;
                    label = rule.label;
                }
            }
        }
        if (resolved > 0) {
            System.getLogger(PreferenceProfile.class.getName())
                    .log(System.Logger.Level.INFO, "偏好命中[{0}]: maxLegKm={1}", label, resolved);
        }
        return resolved;
    }

    /** 兴趣关键词权重：偏好文本命中的兴趣维度加权（未命中的维度给基础权重 0.3） */
    public Map<String, Double> resolveInterestWeights(String preference) {
        String pref = preference == null ? "" : preference;
        Map<String, Double> weights = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : INTEREST_KEYWORDS.entrySet()) {
            // 匹配：维度名或任一关键词出现在偏好文本中（"喜欢历史"命中"历史文化"维度）
            boolean hit = pref.contains(e.getKey())
                    || e.getValue().stream().anyMatch(pref::contains);
            weights.put(e.getKey(), hit ? 1.0 : 0.3);
        }
        return weights;
    }
}
