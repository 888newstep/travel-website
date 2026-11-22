package travel.route.algorithm;

import travel.common.entity.travel_recommendation.Attraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次规划的评分上下文：偏好/预算/约束的落点。
 *
 * <p>设计原则（多偏好与搜索内核正交）：
 * <ul>
 *   <li>兴趣偏好 → <b>选择层</b>：决定"去哪些"，interestScore 供预算修复按性价比淘汰；
 *       排列顺序只影响路网成本，因此 GA fitness 保持距离度量</li>
 *   <li>预算 → <b>选择层修复</b>：票价总和与排列顺序无关，搜索前剪枝（淘汰性价比最低者），
 *       绝不作为排列 fitness 惩罚项（会让搜索空间布满不可行解）</li>
 *   <li>相邻距离上限（带娃/节奏）→ <b>排列层修复</b>：2-opt 后对超长相邻段做 k 近邻交换修复</li>
 * </ul>
 */
public class FitnessContext {

    /** 相邻段距离上限（km），<=0 表示不启用。带娃/慢节奏场景由调用方设置 */
    private final double maxLegKm;

    private final List<Attraction> attractions;
    private final double[] interestScore;
    private final double[] price;
    private final double budget;

    /** 修复过程中被淘汰的景点名（诚实降级：返回给调用方，不悄悄砍需求） */
    private final List<String> droppedByRepair = new ArrayList<>();

    private FitnessContext(List<Attraction> attractions, Map<String, Double> interestWeights,
                           Map<String, List<String>> interestKeywordMap, BigDecimal budget, double maxLegKm) {
        this.attractions = attractions;
        this.budget = budget == null ? 0 : budget.doubleValue();
        this.maxLegKm = maxLegKm;
        this.interestScore = new double[attractions.size()];
        this.price = new double[attractions.size()];
        for (int i = 0; i < attractions.size(); i++) {
            Attraction a = attractions.get(i);
            this.price[i] = a.getTicketPrice() == null ? 0 : a.getTicketPrice().doubleValue();
            String text = (a.getName() == null ? "" : a.getName()) + "|"
                    + (a.getDescription() == null ? "" : a.getDescription());
            double score = 0;
            for (Map.Entry<String, Double> e : interestWeights.entrySet()) {
                List<String> keywords = interestKeywordMap.getOrDefault(e.getKey(), List.of());
                for (String kw : keywords) {
                    if (text.contains(kw)) {
                        score += e.getValue();
                        break;
                    }
                }
            }
            this.interestScore[i] = score;
        }
    }

    public static FitnessContext of(List<Attraction> attractions, Map<String, Double> interestWeights,
                                    Map<String, List<String>> interestKeywordMap,
                                    BigDecimal budget, double maxLegKm) {
        return new FitnessContext(attractions, interestWeights, interestKeywordMap, budget, maxLegKm);
    }

    List<Attraction> getAttractions() {
        return attractions;
    }

    double interestAt(int index) {
        return interestScore[index];
    }

    /** 修复中被淘汰的景点（诚实降级出口） */
    public List<String> getDroppedByRepair() {
        return droppedByRepair;
    }

    /**
     * 预算修复（选择层，GA 之前调用）：总票价超预算时按"兴趣/票价"性价比从低到高淘汰。
     * 票价为 0 的景点不淘汰（免费景点无预算贡献，留着只会提升兴趣分）。
     *
     * @return 预算内的保留子集（保持原顺序）
     */
    public List<Attraction> repairBudget() {
        List<Attraction> kept = new ArrayList<>(attractions);
        if (budget <= 0) {
            return kept;
        }
        double total = pricesum();
        while (total > budget && hasPayable()) {
            int worst = -1;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < attractions.size(); i++) {
                if (price[i] <= 0 || !kept.contains(attractions.get(i))) {
                    continue;
                }
                double ratio = interestScore[i] / price[i];
                if (ratio < worstRatio) {
                    worstRatio = ratio;
                    worst = i;
                }
            }
            if (worst < 0) {
                break;
            }
            kept.remove(attractions.get(worst));
            total -= price[worst];
            droppedByRepair.add(attractions.get(worst).getName() + "(预算超限)");
        }
        return kept;
    }

    private double pricesum() {
        double total = 0;
        for (int i = 0; i < price.length; i++) {
            total += price[i];
        }
        return total;
    }

    private boolean hasPayable() {
        for (double p : price) {
            if (p > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 相邻距离上限修复（排列层，2-opt 之后调用）：
     * 扫描超限相邻段，在超限段后段的 k 近邻中找"能替换该站且不产生新超限"的替代者。
     * 修不动则淘汰该站（记入 droppedByRepair）。
     *
     * @return 修复后的排列（可能少于原数量）
     */
    public List<Attraction> repairMaxLeg(List<Attraction> ordered) {
        if (maxLegKm <= 0 || ordered.size() < 3) {
            return ordered;
        }
        List<Attraction> tour = new ArrayList<>(ordered);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < tour.size() - 1; i++) {
                double d = distance(tour.get(i), tour.get(i + 1));
                if (d <= maxLegKm) {
                    continue;
                }
                // 在较远一端（后继）的替换搜索：尝试与后面更近的站交换
                boolean fixed = false;
                for (int j = i + 2; j < tour.size() && !fixed; j++) {
                    // 交换 i+1 与 j
                    if (distance(tour.get(i), tour.get(j)) <= maxLegKm
                            && distance(tour.get(j), tour.get(i + 1)) <= maxLegKm) {
                        java.util.Collections.swap(tour, i + 1, j);
                        if (!hasAnyLegViolation(tour)) {
                            fixed = true;
                            changed = true;
                        } else {
                            java.util.Collections.swap(tour, i + 1, j);
                        }
                    }
                }
                if (!fixed) {
                    droppedByRepair.add(tour.get(i + 1).getName() + "(相邻距离超限" +
                            String.format("%.1f", d) + "km)");
                    tour.remove(i + 1);
                    changed = true;
                    break; // 重新从头部扫描
                }
            }
        }
        return tour;
    }

    private boolean hasAnyLegViolation(List<Attraction> tour) {
        for (int i = 0; i < tour.size() - 1; i++) {
            if (distance(tour.get(i), tour.get(i + 1)) > maxLegKm) {
                return true;
            }
        }
        return false;
    }

    private double distance(Attraction a, Attraction b) {
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            return 0;
        }
        double latDelta = Math.toRadians(b.getLatitude().doubleValue() - a.getLatitude().doubleValue());
        double lonDelta = Math.toRadians(b.getLongitude().doubleValue() - a.getLongitude().doubleValue());
        double h = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(a.getLatitude().doubleValue()))
                * Math.cos(Math.toRadians(b.getLatitude().doubleValue()))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
