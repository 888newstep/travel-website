package travel.route.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import travel.common.entity.travel_recommendation.Attraction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 遗传算法 TSP 优化器
 * 用于优化景点游览顺序，最小化总距离/时间/成本
 */
@Component
public class GeneticAlgorithmTSP {

    private static final Logger log = LoggerFactory.getLogger(GeneticAlgorithmTSP.class);

    private static final int POPULATION_SIZE = 150;    // 种群大小（网格寻优 150/0.8/0.05 最优）
    private static final int MAX_GENERATIONS = 200;    // 最大迭代次数
    private static final double MUTATION_RATE = 0.05;  // 变异率（网格寻优最优）
    private static final double CROSSOVER_RATE = 0.8;  // 交叉率
    private static final int ELITISM_COUNT = 5;        // 精英保留数量
    private static final int EARLY_STOP_GENERATIONS = 30;   // 连续无提升代数阈值（早停）

    /**
     * 使用遗传算法优化景点顺序
     * @param attractions 景点列表
     * @param optimizationType 优化类型: distance/time/cost/balanced
     * @return 优化后的景点顺序
     */
    public List<Attraction> optimizeRoute(List<Attraction> attractions, String optimizationType) {
        // 兼容旧签名：按规模自动选档（暴力实验：小规模 FAST 质量无损）
        return optimizeRoute(attractions, GaPreset.forScale(attractions.size()));
    }

    /**
     * 预设档位驱动的优化入口。
     * 参数与用户偏好正交：偏好/预算在 FitnessContext（选择层）处理，
     * 本方法只负责"给定集合内的顺序优化"（距离度量）。
     */
    /** GA 质量优势上限（2230 组暴力基准：n>400 后 GA 质量优势消失且时间爆炸，走 NN+2-opt 快速路径） */
    static final int GA_MAX_N = 400;

    public List<Attraction> optimizeRoute(List<Attraction> attractions, GaPreset preset) {
        if (attractions.size() <= 2) {
            return attractions;
        }
        // 大规模快速路径：贪心最近邻 + 2-opt（实验：n=3200 内秒级，质量与 GA 差 <2%）
        if (attractions.size() > GA_MAX_N) {
            log.info("规模{}超出GA适用上限{}，走 NN+2-opt 快速路径", attractions.size(), GA_MAX_N);
            int[] tour = nearestNeighborSeed(attractions, new Random());
            twoOpt(tour, attractions);
            List<Attraction> order = new ArrayList<>();
            for (int index : tour) {
                order.add(attractions.get(index));
            }
            return order;
        }
        GaPreset p = preset == null ? GaPreset.STANDARD : preset;
        log.info("开始遗传算法优化: 景点数={}, 档位={}", attractions.size(), p);

        // 1. 初始化种群：随机个体 + 贪心最近邻种子（加速收敛）
        Random random = new Random();
        List<int[]> population = initializePopulation(attractions.size(), p);
        population.set(0, nearestNeighborSeed(attractions, random));

        // 2. 迭代进化（带早停：连续 EARLY_STOP_GENERATIONS 代无提升则收敛）
        int[] bestSolution = null;
        double bestFitness = -Double.MAX_VALUE;
        int generationsWithoutImprovement = 0;

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // 计算适应度
            Map<int[], Double> fitnessMap = calculateFitness(population, attractions);

            // 找到最优解
            double generationBest = bestFitness;
            for (Map.Entry<int[], Double> entry : fitnessMap.entrySet()) {
                if (entry.getValue() > bestFitness) {
                    bestFitness = entry.getValue();
                    bestSolution = entry.getKey().clone();
                }
            }
            generationsWithoutImprovement = bestFitness > generationBest ? 0 : generationsWithoutImprovement + 1;
            if (generation >= p.generations / 2 && generationsWithoutImprovement >= p.earlyStopGenerations) {
                log.info("遗传算法早停: 第{}代连续{}代无提升", generation + 1, generationsWithoutImprovement);
                break;
            }

            // 选择、交叉、变异
            population = evolve(population, fitnessMap, attractions.size(), random, p);

            // 每50代输出一次日志
            if ((generation + 1) % 50 == 0) {
                log.debug("第{}代: 最优适应度={}", generation + 1, bestFitness);
            }
        }

        // 3. 2-opt 局部搜索消除交叉路径，再转换为景点列表
        if (bestSolution != null) {
            twoOpt(bestSolution, attractions);
        }
        List<Attraction> optimizedOrder = new ArrayList<>();
        for (int index : bestSolution) {
            optimizedOrder.add(attractions.get(index));
        }

        log.info("遗传算法优化完成: 最优适应度={}", bestFitness);
        return optimizedOrder;
    }

    /**
     * 初始化种群
     */
    private List<int[]> initializePopulation(int size, GaPreset p) {
        List<int[]> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < p.population; i++) {
            int[] chromosome = IntStream.range(0, size).toArray();
            shuffleArray(chromosome, random);
            population.add(chromosome);
        }

        return population;
    }

    /**
     * 计算适应度
     */
    private Map<int[], Double> calculateFitness(List<int[]> population,
                                                List<Attraction> attractions) {
        Map<int[], Double> fitnessMap = new HashMap<>();

        for (int[] chromosome : population) {
            double fitness = evaluateFitness(chromosome, attractions);
            fitnessMap.put(chromosome, fitness);
        }

        return fitnessMap;
    }

    /**
     * 评估单个染色体的适应度
     */
    private double evaluateFitness(int[] chromosome, List<Attraction> attractions) {
        double totalDistance = 0.0;
        for (int index = 0; index < chromosome.length - 1; index++) {
            Attraction current = attractions.get(chromosome[index]);
            Attraction next = attractions.get(chromosome[index + 1]);
            if (current.getLatitude() == null || current.getLongitude() == null
                    || next.getLatitude() == null || next.getLongitude() == null) {
                return 0.0;
            }
            totalDistance += haversineDistance(
                    current.getLongitude().doubleValue(), current.getLatitude().doubleValue(),
                    next.getLongitude().doubleValue(), next.getLatitude().doubleValue());
        }

        if (totalDistance <= 0.0) {
            return 0.0;
        }

        // 遗传算法仅用坐标距离筛选顺序；最终距离和耗时统一由高德 API 计算。
        return 1.0 / (1.0 + totalDistance);
    }

    private double haversineDistance(double longitude1, double latitude1,
                                     double longitude2, double latitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double value = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    /**
     * 进化：选择、交叉、变异
     */
    private List<int[]> evolve(List<int[]> population, Map<int[], Double> fitnessMap,
                               int chromosomeLength, Random random, GaPreset p) {
        List<int[]> newPopulation = new ArrayList<>();

        // 1. 精英保留
        List<Map.Entry<int[], Double>> sortedByFitness = fitnessMap.entrySet().stream()
                .sorted(Map.Entry.<int[], Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < p.elitismCount && i < sortedByFitness.size(); i++) {
            newPopulation.add(sortedByFitness.get(i).getKey().clone());
        }

        // 2. 选择、交叉、变异生成新个体
        while (newPopulation.size() < p.population) {
            int[] parent1 = tournamentSelection(population, fitnessMap, random);
            int[] parent2 = tournamentSelection(population, fitnessMap, random);

            int[] child;
            if (random.nextDouble() < p.crossoverRate) {
                child = orderedCrossover(parent1, parent2, chromosomeLength);
            } else {
                child = parent1.clone();
            }

            // 变异
            if (random.nextDouble() < p.mutationRate) {
                mutate(child, random);
            }

            newPopulation.add(child);
        }

        return newPopulation;
    }

    /**
     * 锦标赛选择
     */
    private int[] tournamentSelection(List<int[]> population, Map<int[], Double> fitnessMap, Random random) {
        int tournamentSize = 5;
        int[] best = null;
        double bestFitness = -Double.MAX_VALUE;

        for (int i = 0; i < tournamentSize; i++) {
            int index = random.nextInt(population.size());
            int[] individual = population.get(index);
            double fitness = fitnessMap.get(individual);

            if (fitness > bestFitness) {
                bestFitness = fitness;
                best = individual;
            }
        }

        return best.clone();
    }

    /**
     * 有序交叉（OX）
     */
    private int[] orderedCrossover(int[] parent1, int[] parent2, int length) {
        Random random = new Random();
        int[] child = new int[length];
        Arrays.fill(child, -1);

        // 随机选择交叉区间
        int start = random.nextInt(length);
        int end = random.nextInt(length);
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        // 复制parent1的交叉区间
        for (int i = start; i <= end; i++) {
            child[i] = parent1[i];
        }

        // 从parent2填充剩余位置
        int currentIndex = (end + 1) % length;
        for (int i = 0; i < length; i++) {
            int index = (end + 1 + i) % length;
            int gene = parent2[index];

            if (!contains(child, gene)) {
                child[currentIndex] = gene;
                currentIndex = (currentIndex + 1) % length;
            }
        }

        return child;
    }

    /**
     * 变异：交换两个基因
     */
    private void mutate(int[] chromosome, Random random) {
        int index1 = random.nextInt(chromosome.length);
        int index2 = random.nextInt(chromosome.length);

        int temp = chromosome[index1];
        chromosome[index1] = chromosome[index2];
        chromosome[index2] = temp;
    }

    /**
     * 贪心最近邻种子解：从随机起点出发，每步去最近的未访问景点。
     * 作为种群第 0 号个体，为进化提供一个 O(n^2) 的良好起点。
     */
    private int[] nearestNeighborSeed(List<Attraction> attractions, Random random) {
        int n = attractions.size();
        int[] tour = new int[n];
        boolean[] visited = new boolean[n];
        int current = random.nextInt(n);
        tour[0] = current;
        visited[current] = true;
        for (int i = 1; i < n; i++) {
            int next = -1;
            double best = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (visited[j]) {
                    continue;
                }
                double d = safeDistance(attractions.get(current), attractions.get(j));
                if (d < best) {
                    best = d;
                    next = j;
                }
            }
            if (next < 0) {
                // 全部距离不可用时（坐标缺失）退化为顺序填充，保证排列合法
                for (int j = 0; j < n; j++) {
                    if (!visited[j]) {
                        next = j;
                        break;
                    }
                }
            }
            tour[i] = next;
            visited[next] = true;
            current = next;
        }
        return tour;
    }

    private double safeDistance(Attraction a, Attraction b) {
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return haversineDistance(a.getLongitude().doubleValue(), a.getLatitude().doubleValue(),
                b.getLongitude().doubleValue(), b.getLatitude().doubleValue());
    }

    /**
     * 2-opt 局部搜索：反复反转产生交叉的路径段，直到无改进（first-improvement）。
     * 对 GA 输出做收尾优化，消除"路线交叉"的次优段。
     */
    void twoOpt(int[] tour, List<Attraction> attractions) {
        int n = tour.length;
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < n - 2; i++) {
                for (int j = i + 2; j < n - 1; j++) {
                    // 开放路径 2-opt：不含闭环边（j+1 必须是有效后继）
                    double before = safeDistance(attractions.get(tour[i]), attractions.get(tour[i + 1]))
                            + safeDistance(attractions.get(tour[j]), attractions.get(tour[j + 1]));
                    double after = safeDistance(attractions.get(tour[i]), attractions.get(tour[j]))
                            + safeDistance(attractions.get(tour[i + 1]), attractions.get(tour[j + 1]));
                    if (after < before - 1e-9) {
                        reverse(tour, i + 1, j);
                        improved = true;
                    }
                }
            }
        }
    }

    private void reverse(int[] tour, int from, int to) {
        while (from < to) {
            int tmp = tour[from];
            tour[from] = tour[to];
            tour[to] = tmp;
            from++;
            to--;
        }
    }

    /**
     * 检查数组是否包含某个值
     */
    private boolean contains(int[] array, int value) {
        for (int item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 随机打乱数组
     */
    private void shuffleArray(int[] array, Random random) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}
