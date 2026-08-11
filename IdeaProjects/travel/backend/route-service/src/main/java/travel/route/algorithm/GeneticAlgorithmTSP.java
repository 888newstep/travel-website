package travel.route.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.AMapRouteService;

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

    private static final int POPULATION_SIZE = 100;    // 种群大小
    private static final int MAX_GENERATIONS = 200;    // 最大迭代次数
    private static final double MUTATION_RATE = 0.02;  // 变异率
    private static final double CROSSOVER_RATE = 0.8;  // 交叉率
    private static final int ELITISM_COUNT = 5;        // 精英保留数量

    private final AMapRouteService aMapRouteService;

    public GeneticAlgorithmTSP(AMapRouteService aMapRouteService) {
        this.aMapRouteService = aMapRouteService;
    }

    /**
     * 使用遗传算法优化景点顺序
     * @param attractions 景点列表
     * @param optimizationType 优化类型: distance/time/cost/balanced
     * @return 优化后的景点顺序
     */
    public List<Attraction> optimizeRoute(List<Attraction> attractions, String optimizationType) {
        if (attractions.size() <= 2) {
            return attractions;
        }

        log.info("开始遗传算法优化: 景点数={}, 优化类型={}", attractions.size(), optimizationType);

        // 1. 初始化种群
        List<int[]> population = initializePopulation(attractions.size());

        // 2. 迭代进化
        int[] bestSolution = null;
        double bestFitness = Double.MIN_VALUE;

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // 计算适应度
            Map<int[], Double> fitnessMap = calculateFitness(population, attractions, optimizationType);

            // 找到最优解
            for (Map.Entry<int[], Double> entry : fitnessMap.entrySet()) {
                if (entry.getValue() > bestFitness) {
                    bestFitness = entry.getValue();
                    bestSolution = entry.getKey().clone();
                }
            }

            // 选择、交叉、变异
            population = evolve(population, fitnessMap, attractions.size());

            // 每50代输出一次日志
            if ((generation + 1) % 50 == 0) {
                log.debug("第{}代: 最优适应度={}", generation + 1, bestFitness);
            }
        }

        // 3. 将最优解转换为景点列表
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
    private List<int[]> initializePopulation(int size) {
        List<int[]> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
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
                                                List<Attraction> attractions,
                                                String optimizationType) {
        Map<int[], Double> fitnessMap = new HashMap<>();

        for (int[] chromosome : population) {
            double fitness = evaluateFitness(chromosome, attractions, optimizationType);
            fitnessMap.put(chromosome, fitness);
        }

        return fitnessMap;
    }

    /**
     * 评估单个染色体的适应度
     */
    private double evaluateFitness(int[] chromosome, List<Attraction> attractions, String optimizationType) {
        List<double[]> coordinates = new ArrayList<>();
        for (int index : chromosome) {
            Attraction attraction = attractions.get(index);
            if (attraction.getLatitude() != null && attraction.getLongitude() != null) {
                coordinates.add(new double[]{
                        attraction.getLongitude().doubleValue(),
                        attraction.getLatitude().doubleValue()
                });
            }
        }

        if (coordinates.size() < 2) {
            return 0.0;
        }

        // 调用高德地图获取真实路径信息
        AMapRouteService.RouteInfo routeInfo = aMapRouteService.calculateMultiPointRoute(coordinates);

        if (routeInfo == null) {
            return 0.0;
        }

        // 根据优化类型计算适应度
        double fitness;
        switch (optimizationType.toLowerCase()) {
            case "distance":
                fitness = 1.0 / (1.0 + routeInfo.getDistance());
                break;
            case "time":
                fitness = 1.0 / (1.0 + routeInfo.getDuration());
                break;
            case "cost":
                fitness = 1.0 / (1.0 + routeInfo.getCost());
                break;
            case "balanced":
            default:
                fitness = 1.0 / (1.0 + routeInfo.getDistance() * 0.4 +
                        routeInfo.getDuration() * 0.3 +
                        routeInfo.getCost() * 0.3);
                break;
        }

        return fitness;
    }

    /**
     * 进化：选择、交叉、变异
     */
    private List<int[]> evolve(List<int[]> population, Map<int[], Double> fitnessMap, int chromosomeLength) {
        List<int[]> newPopulation = new ArrayList<>();
        Random random = new Random();

        // 1. 精英保留
        List<Map.Entry<int[], Double>> sortedByFitness = fitnessMap.entrySet().stream()
                .sorted(Map.Entry.<int[], Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < ELITISM_COUNT && i < sortedByFitness.size(); i++) {
            newPopulation.add(sortedByFitness.get(i).getKey().clone());
        }

        // 2. 选择、交叉、变异生成新个体
        while (newPopulation.size() < POPULATION_SIZE) {
            int[] parent1 = tournamentSelection(population, fitnessMap, random);
            int[] parent2 = tournamentSelection(population, fitnessMap, random);

            int[] child;
            if (random.nextDouble() < CROSSOVER_RATE) {
                child = orderedCrossover(parent1, parent2, chromosomeLength);
            } else {
                child = parent1.clone();
            }

            // 变异
            if (random.nextDouble() < MUTATION_RATE) {
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
        double bestFitness = Double.MIN_VALUE;

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
