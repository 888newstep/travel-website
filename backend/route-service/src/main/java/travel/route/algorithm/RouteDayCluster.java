package travel.route.algorithm;

import travel.common.entity.travel_recommendation.Attraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 地理 K-means 均衡分天聚类。
 *
 * <p>替代朴素顺序切块：将 n 个景点按经纬度聚成 maxDays 个地理簇，
 * 使每天的景点在空间上相邻，消除"同天跨城折返"。
 * 均衡约束：每簇容量上限为 ceil(n/maxDays)，超簇最远点迁移到最近的未满簇。
 */
public final class RouteDayCluster {

    private RouteDayCluster() {
    }

    /**
     * 将景点按地理坐标聚成 maxDays 组，返回按天分组结果（day 从 1 开始）。
     * 景点数不足 maxDays 时，每天最多一个景点。
     */
    public static List<List<Attraction>> clusterByDay(List<Attraction> attractions, int maxDays, long seed) {
        int days = Math.min(maxDays, attractions.size());
        List<List<Attraction>> clusters = new ArrayList<>();
        if (days <= 0) {
            return clusters;
        }
        if (days == 1) {
            clusters.add(new ArrayList<>(attractions));
            return clusters;
        }

        List<double[]> points = attractions.stream()
                .map(a -> new double[]{a.getLongitude().doubleValue(), a.getLatitude().doubleValue()})
                .toList();

        // k-means++ 选初始中心，再 Lloyd 迭代
        List<double[]> centers = kMeansPlusPlusSeeds(points, days, seed);
        int[] assignment = new int[points.size()];
        for (int iter = 0; iter < 50; iter++) {
            boolean changed = assign(points, centers, assignment);
            updateCenters(points, assignment, centers);
            if (!changed) {
                break;
            }
        }

        // 均衡化：每簇容量上限 ceil(n/days)，超簇最远点迁移到最近未满簇
        int cap = (int) Math.ceil((double) points.size() / days);
        List<List<Integer>> members = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            members.add(new ArrayList<>());
        }
        for (int i = 0; i < points.size(); i++) {
            members.get(assignment[i]).add(i);
        }
        balance(points, centers, members, cap);

        for (List<Integer> day : members) {
            List<Attraction> group = new ArrayList<>();
            for (int idx : day) {
                group.add(attractions.get(idx));
            }
            clusters.add(group);
        }
        return clusters;
    }

    private static List<double[]> kMeansPlusPlusSeeds(List<double[]> points, int k, long seed) {
        Random random = new Random(seed);
        List<double[]> centers = new ArrayList<>();
        centers.add(points.get(random.nextInt(points.size())).clone());
        while (centers.size() < k) {
            double[] distSq = new double[points.size()];
            double total = 0;
            for (int i = 0; i < points.size(); i++) {
                double min = Double.MAX_VALUE;
                for (double[] c : centers) {
                    double d = dist(points.get(i), c);
                    if (d < min) {
                        min = d;
                    }
                }
                distSq[i] = min * min;
                total += distSq[i];
            }
            double r = random.nextDouble() * total;
            int chosen = points.size() - 1;
            for (int i = 0; i < points.size(); i++) {
                r -= distSq[i];
                if (r <= 0) {
                    chosen = i;
                    break;
                }
            }
            centers.add(points.get(chosen).clone());
        }
        return centers;
    }

    private static boolean assign(List<double[]> points, List<double[]> centers, int[] assignment) {
        boolean changed = false;
        for (int i = 0; i < points.size(); i++) {
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int c = 0; c < centers.size(); c++) {
                double d = dist(points.get(i), centers.get(c));
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
            if (assignment[i] != best) {
                assignment[i] = best;
                changed = true;
            }
        }
        return changed;
    }

    private static void updateCenters(List<double[]> points, int[] assignment, List<double[]> centers) {
        for (int c = 0; c < centers.size(); c++) {
            double sx = 0, sy = 0;
            int count = 0;
            for (int i = 0; i < points.size(); i++) {
                if (assignment[i] == c) {
                    sx += points.get(i)[0];
                    sy += points.get(i)[1];
                    count++;
                }
            }
            if (count > 0) {
                centers.get(c)[0] = sx / count;
                centers.get(c)[1] = sy / count;
            }
        }
    }

    /**
     * 容量均衡：把超出 cap 的簇中最远离簇心的点，迁移到最近的未满簇。
     */
    private static void balance(List<double[]> points, List<double[]> centers,
                                List<List<Integer>> members, int cap) {
        boolean moved = true;
        while (moved) {
            moved = false;
            for (int c = 0; c < members.size(); c++) {
                while (members.get(c).size() > cap) {
                    int farthest = -1;
                    double farthestDist = -1;
                    for (int idx : members.get(c)) {
                        double d = dist(points.get(idx), centers.get(c));
                        if (d > farthestDist) {
                            farthestDist = d;
                            farthest = idx;
                        }
                    }
                    int target = -1;
                    double targetDist = Double.MAX_VALUE;
                    for (int t = 0; t < members.size(); t++) {
                        if (t == c || members.get(t).size() >= cap) {
                            continue;
                        }
                        double d = dist(points.get(farthest), centers.get(t));
                        if (d < targetDist) {
                            targetDist = d;
                            target = t;
                        }
                    }
                    if (target < 0) {
                        return;
                    }
                    members.get(c).remove(Integer.valueOf(farthest));
                    members.get(target).add(farthest);
                    moved = true;
                }
            }
        }
    }

    /**
     * 经纬度近似平面距离（聚类用，精度要求低于 Haversine，避免三角函数开销 ×50 次迭代）
     */
    private static double dist(double[] a, double[] b) {
        double dx = (a[0] - b[0]) * 111.32 * Math.cos(Math.toRadians((a[1] + b[1]) / 2));
        double dy = (a[1] - b[1]) * 110.57;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
