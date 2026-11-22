package travel.route.service.support;

import org.springframework.stereotype.Component;
import travel.common.entity.travel_recommendation.Attraction;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 营业时间窗后置校验 + 贪心修复。
 *
 * <p>设计取舍（TSPTW 不进 GA 的原因）：2-opt 反转会破坏后继所有站的时间窗，
 * 修复传播代价远超收益——所以 GA/2-opt 只管空间成本，时间窗在排完后做一次
 * 线性扫描 + 近邻交换贪心修复；修不动的站进 unsatisfiable 列表诚实降级。
 *
 * <p>时间模型（粗排层无真实路网耗时，用保守估算）：
 * start=09:00，每站默认游玩 90 分钟，站间交通 = 距离km / 40km/h * 60 分钟。
 */
@Component
public class RouteScheduleValidator {

    private static final int DAY_START_MINUTES = 9 * 60;
    private static final int DEFAULT_VISIT_MINUTES = 90;
    private static final int SWAP_RADIUS = 3;

    public record Result(List<String> orderedNames, List<String> unsatisfiable, int swaps) {
    }

    /**
     * @param attractions GA 优化后的当日顺序
     * @return 修复后的顺序 + 无法满足时间窗被降级的站点
     */
    public Result validateAndFix(List<Attraction> attractions) {
        List<Attraction> tour = new ArrayList<>(attractions);
        List<String> unsatisfiable = new ArrayList<>();
        int swaps = 0;

        for (int i = 0; i < tour.size(); ) {
            Attraction a = tour.get(i);
            Window w = parseWindow(a.getOpeningHours());
            if (w == null || fits(tour, i, w)) {
                i++;
                continue;
            }
            // 贪心修复：半径内双向交换（违规站可能需要"提前"而非"延后"），
            // 准入条件 = 交换后全程无时间窗违规（fitsAll，n<=50 代价可忽略）
            boolean fixed = false;
            int lo = Math.max(0, i - SWAP_RADIUS), hi = Math.min(tour.size() - 1, i + SWAP_RADIUS);
            for (int j = lo; j <= hi && !fixed; j++) {
                if (j == i) continue;  // null 时间窗站点可参与交换，由 fitsAll 统一裁决
                java.util.Collections.swap(tour, i, j);
                if (fitsAll(tour)) {
                    swaps++;
                    fixed = true;
                } else {
                    java.util.Collections.swap(tour, i, j);
                }
            }
            if (!fixed) {
                unsatisfiable.add(a.getName() + "(时间窗不满足:" + a.getOpeningHours() + ")");
                tour.remove(i);
            } else {
                i++;
            }
        }
        List<String> names = tour.stream().map(Attraction::getName).toList();
        return new Result(names, unsatisfiable, swaps);
    }

    private boolean fitsAll(List<Attraction> tour) {
        for (int i = 0; i < tour.size(); i++) {
            Window w = parseWindow(tour.get(i).getOpeningHours());
            if (w != null && !fits(tour, i, w)) {
                return false;
            }
        }
        return true;
    }

    /** 模拟到站 i 时刻是否落在其时间窗内（按 Day start + 前序游玩/交通累加） */
    private boolean fits(List<Attraction> tour, int i, Window window) {
        int clock = DAY_START_MINUTES;
        for (int k = 0; k <= i; k++) {
            if (k > 0) {
                double km = distanceKm(tour.get(k - 1), tour.get(k));
                clock += (int) Math.round(km / 40.0 * 60);
            }
            if (k == i) {
                break;
            }
            clock += DEFAULT_VISIT_MINUTES;
        }
        // 物理语义：早到可等待（合法），只有到达时间晚于结束时间才是真违规
        return clock <= window.end;
    }

    /** openingHours 解析 "HH:mm-HH:mm"，无法解析（全天/休息中/空）返回 null = 不设窗 */
    static Window parseWindow(String openingHours) {
        if (openingHours == null || openingHours.isBlank()) {
            return null;
        }
        String[] parts = openingHours.split("[-~—]");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Window(LocalTime.parse(parts[0].trim()).toSecondOfDay() / 60,
                    LocalTime.parse(parts[1].trim()).toSecondOfDay() / 60);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private double distanceKm(Attraction a, Attraction b) {
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

    record Window(int start, int end) {
    }
}
