package travel.route.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.route.dto.ai.AIActivity;
import travel.route.dto.ai.AIDailyPlan;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AITimeWindow;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 路线规划约束调度器。
 *
 * <p>当前实现是确定性规则调度器，负责把请求约束落实到每日活动结果；后续可替换为
 * 更复杂的搜索、遗传算法或外部地图规划器，而不扩大 AIAdvancedServiceImpl 的职责。</p>
 */
public class AIPlanConstraintScheduler {

    private static final Logger log = LoggerFactory.getLogger(AIPlanConstraintScheduler.class);

    private static final int DEFAULT_MAX_DAILY_MINUTES = 8 * 60;
    private static final int MIN_DAILY_MINUTES = 60;
    private static final int MAX_DAILY_MINUTES = 24 * 60;

    private static final List<ActivityTemplate> ACTIVITY_TEMPLATES = List.of(
            new ActivityTemplate("09:00-12:00", 180, "attraction", "景点%d-上午", "上午游览当地著名景点"),
            new ActivityTemplate("12:00-13:30", 90, "restaurant", "餐厅%d", "品尝当地特色美食"),
            new ActivityTemplate("14:00-17:00", 180, "attraction", "景点%d-下午", "下午参观文化景点")
    );

    /**
     * 根据约束生成每日计划。
     *
     * <p>maxDailyHours 表示每日实际活动总时长，不包含活动之间的空档时间。</p>
     */
    public List<AIDailyPlan> buildDailyPlans(int days, AIPlanRouteConstraints constraints) {
        ConstraintContext context = ConstraintContext.from(constraints);
        validateConflicts(context);

        List<AIDailyPlan> dailyPlans = new ArrayList<>(days);
        int[] nextMustVisitIndex = {0};
        for (int day = 1; day <= days; day++) {
            dailyPlans.add(buildDailyPlan(day, context, nextMustVisitIndex));
        }

        if (nextMustVisitIndex[0] < context.mustVisitAttractions().size()) {
            List<String> missing = context.mustVisitAttractions()
                    .subList(nextMustVisitIndex[0], context.mustVisitAttractions().size());
            throw new IllegalArgumentException("必游景点无法在当前路线约束下全部安排: " + String.join("、", missing));
        }
        return dailyPlans;
    }

    private AIDailyPlan buildDailyPlan(int day, ConstraintContext context, int[] nextMustVisitIndex) {
        List<AIActivity> activities = new ArrayList<>(ACTIVITY_TEMPLATES.size());
        DailyScheduleCursor cursor = new DailyScheduleCursor(context.timeWindows());
        int usedMinutes = 0;

        for (ActivityTemplate template : ACTIVITY_TEMPLATES) {
            boolean attraction = "attraction".equals(template.type());
            boolean mustVisit = attraction
                    && nextMustVisitIndex[0] < context.mustVisitAttractions().size();
            String activityName = mustVisit
                    ? context.mustVisitAttractions().get(nextMustVisitIndex[0])
                    : String.format(Locale.ROOT, template.nameTemplate(), day);

            if (attraction && !mustVisit && isAvoided(activityName, context.avoidAttractions())) {
                log.debug("跳过被避开的默认景点: day={}, name={}", day, activityName);
                continue;
            }

            if (usedMinutes + template.durationMinutes() > context.maxDailyMinutes()) {
                log.debug("活动超过每日时长约束，跳过: day={}, name={}", day, activityName);
                continue;
            }

            String time = cursor.next(template.durationMinutes(), template.defaultTime());
            if (time == null) {
                log.debug("活动无法放入固定时间窗口，跳过: day={}, name={}", day, activityName);
                continue;
            }

            activities.add(new AIActivity(time, template.type(), activityName, template.description()));
            usedMinutes += template.durationMinutes();
            if (mustVisit) {
                nextMustVisitIndex[0]++;
            }
        }

        return new AIDailyPlan(day, "第" + day + "天行程", activities);
    }

    private void validateConflicts(ConstraintContext context) {
        for (String mustVisit : context.mustVisitAttractions()) {
            if (isAvoided(mustVisit, context.avoidAttractions())) {
                throw new IllegalArgumentException("必游景点与避开景点冲突: " + mustVisit);
            }
        }
    }

    private boolean isAvoided(String attractionName, Set<String> avoidAttractions) {
        String normalizedName = normalizeName(attractionName);
        return avoidAttractions.stream().anyMatch(normalizedName::contains);
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ActivityTemplate(String defaultTime, int durationMinutes, String type,
                                    String nameTemplate, String description) {
    }

    private record TimeRange(int startMinute, int endMinute) {
    }

    private record ConstraintContext(int maxDailyMinutes, List<String> mustVisitAttractions,
                                    Set<String> avoidAttractions, List<TimeRange> timeWindows) {

        private static ConstraintContext from(AIPlanRouteConstraints constraints) {
            if (constraints == null) {
                return new ConstraintContext(DEFAULT_MAX_DAILY_MINUTES, List.of(), Set.of(), List.of());
            }

            int maxDailyMinutes = normalizeMaxDailyMinutes(constraints.getMaxDailyHours());
            List<String> mustVisitAttractions = normalizeNames(constraints.getMustVisitAttractions());
            Set<String> avoidAttractions = new HashSet<>(normalizeNames(constraints.getAvoidAttractions())
                    .stream()
                    .map(AIPlanConstraintScheduler::normalizeName)
                    .toList());
            List<TimeRange> timeWindows = normalizeTimeWindows(constraints.getFixedTimeWindows());
            return new ConstraintContext(maxDailyMinutes, mustVisitAttractions, avoidAttractions, timeWindows);
        }

        private static int normalizeMaxDailyMinutes(Integer maxDailyHours) {
            if (maxDailyHours == null) {
                return DEFAULT_MAX_DAILY_MINUTES;
            }
            long requestedMinutes = maxDailyHours.longValue() * 60L;
            return (int) Math.max(MIN_DAILY_MINUTES, Math.min(MAX_DAILY_MINUTES, requestedMinutes));
        }

        private static List<String> normalizeNames(List<String> names) {
            if (names == null || names.isEmpty()) {
                return List.of();
            }
            Map<String, String> uniqueNames = new LinkedHashMap<>();
            for (String name : names) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                String normalized = normalizeName(name);
                uniqueNames.putIfAbsent(normalized, name.trim());
            }
            return List.copyOf(uniqueNames.values());
        }

        private static List<TimeRange> normalizeTimeWindows(List<AITimeWindow> windows) {
            if (windows == null || windows.isEmpty()) {
                return List.of();
            }

            List<TimeRange> sorted = new ArrayList<>(windows.size());
            for (AITimeWindow window : windows) {
                if (window == null || window.getStart() == null || window.getEnd() == null) {
                    throw new IllegalArgumentException("固定时间窗口不能为空");
                }
                try {
                    int start = toMinute(LocalTime.parse(window.getStart()));
                    int end = toMinute(LocalTime.parse(window.getEnd()));
                    if (end <= start) {
                        throw new IllegalArgumentException("固定时间窗口结束时间必须晚于开始时间");
                    }
                    sorted.add(new TimeRange(start, end));
                } catch (java.time.format.DateTimeParseException e) {
                    throw new IllegalArgumentException("固定时间窗口必须使用 HH:mm 格式", e);
                }
            }

            sorted.sort(Comparator.comparingInt(TimeRange::startMinute));
            List<TimeRange> merged = new ArrayList<>(sorted.size());
            for (TimeRange current : sorted) {
                if (merged.isEmpty()) {
                    merged.add(current);
                    continue;
                }
                TimeRange previous = merged.get(merged.size() - 1);
                if (current.startMinute() <= previous.endMinute()) {
                    merged.set(merged.size() - 1,
                            new TimeRange(previous.startMinute(), Math.max(previous.endMinute(), current.endMinute())));
                } else {
                    merged.add(current);
                }
            }
            return List.copyOf(merged);
        }

        private static int toMinute(LocalTime time) {
            return time.getHour() * 60 + time.getMinute();
        }
    }

    private static final class DailyScheduleCursor {
        private final List<TimeRange> timeWindows;
        private int windowIndex;
        private int cursorMinute;

        private DailyScheduleCursor(List<TimeRange> timeWindows) {
            this.timeWindows = timeWindows;
        }

        private String next(int durationMinutes, String defaultTime) {
            if (timeWindows.isEmpty()) {
                return defaultTime;
            }

            for (int candidateIndex = windowIndex; candidateIndex < timeWindows.size(); candidateIndex++) {
                TimeRange window = timeWindows.get(candidateIndex);
                int start = candidateIndex == windowIndex
                        ? Math.max(window.startMinute(), cursorMinute)
                        : window.startMinute();
                if (start + durationMinutes > window.endMinute()) {
                    continue;
                }
                windowIndex = candidateIndex;
                cursorMinute = start + durationMinutes;
                return formatTimeRange(start, cursorMinute);
            }
            return null;
        }

        private static String formatTimeRange(int startMinute, int endMinute) {
            return String.format(Locale.ROOT, "%02d:%02d-%02d:%02d",
                    startMinute / 60, startMinute % 60, endMinute / 60, endMinute % 60);
        }
    }
}
