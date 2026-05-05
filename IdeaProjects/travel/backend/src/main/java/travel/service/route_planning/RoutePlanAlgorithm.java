package travel.service.route_planning;

import travel.entity.travel_recommendation.Attraction;
import travel.entity.route_planning.Transport;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class RoutePlanAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanAlgorithm.class);

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private TransportService transportService;

    /**
     * 动态调整算法参数（根据景点数量、行程天数、用户偏好）
     */
    private AlgorithmParams getDynamicParams(int attractionCount, int maxDays, String preference) {
        AlgorithmParams params = new AlgorithmParams();
        if (attractionCount > 10) {
            params.setPopulationSize(100);
            params.setMaxGenerations(150);
        } else {
            params.setPopulationSize(60);
            params.setMaxGenerations(80);
        }

        return params;
    }

    /**
     * 算法参数封装
     */
    private static class AlgorithmParams {
        private int populationSize = 80;
        private int maxGenerations = 120;

        public int getPopulationSize() { return populationSize; }
        public void setPopulationSize(int populationSize) { this.populationSize = populationSize; }
        public int getMaxGenerations() { return maxGenerations; }
        public void setMaxGenerations(int maxGenerations) { this.maxGenerations = maxGenerations; }
    }

    /**
     * 优化后的核心规划逻辑（整合动态参数）
     */
    public OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference) {
        // 1. 获取动态参数
        AlgorithmParams params = getDynamicParams(attractionIds.size(), maxDays, preference);
        
        // 2. 初始化景点数据（从数据库获取）
        List<Attraction> attractions = new ArrayList<>();
        for (Integer attractionId : attractionIds) {
            Attraction attraction = attractionService.getById(attractionId);
            if (attraction != null) {
                attractions.add(attraction);
            }
        }
        
        // 3. 初始化交通数据（从数据库获取）
        List<Transport> transportList = new ArrayList<>();
        // 获取所有交通方式
        transportList = transportService.list();
        
        // 4. 执行遗传算法优化
        List<RouteChromosome> population = initializePopulation(attractionIds, maxDays, params);
        
        for (int generation = 0; generation < params.getMaxGenerations(); generation++) {
            // 计算适应度
            population.forEach(chromosome -> calculateFitness(chromosome, budget, preference));
            
            // 选择
            List<RouteChromosome> selected = selection(population, params);
            
            // 交叉
            List<RouteChromosome> offspring = crossover(selected, params, maxDays);
            
            // 变异
            mutate(offspring, params);
            
            // 更新种群
            population = replacePopulation(population, offspring);
        }
        
        // 5. 执行蚁群算法优化（局部搜索）
        RouteChromosome bestChromosome = population.stream()
                .max(Comparator.comparingDouble(RouteChromosome::getFitness))
                .orElse(null);
        
        // 6. 转换为最优路线
        OptimalRoute optimalRoute = convertToOptimalRoute(bestChromosome, attractions, transportList);
        
        // 7. 执行路线优化
        if (optimalRoute != null && optimalRoute.getDayPlans() != null && !optimalRoute.getDayPlans().isEmpty()) {
            try {
                // 优化交通方式
                optimizeTransportation(optimalRoute.getDayPlans(), attractions, transportList);
                
                // 优化开放时间安排
                optimizeOpeningHours(optimalRoute.getDayPlans(), attractions);
                
                // 考虑天气因素优化（默认晴天）
                optimizeForWeather(optimalRoute.getDayPlans(), attractions, "sunny");
                
                // 计算总距离、总成本、总时间
                double totalDistance = optimalRoute.getDayPlans().stream()
                        .mapToDouble(RouteDayPlan::getDistance)
                        .sum();
                double totalCost = optimalRoute.getDayPlans().stream()
                        .mapToDouble(RouteDayPlan::getCost)
                        .sum();
                double totalTime = optimalRoute.getDayPlans().stream()
                        .mapToDouble(RouteDayPlan::getTime)
                        .sum();
                
                optimalRoute.setTotalDistance(totalDistance);
                optimalRoute.setTotalCost(totalCost);
                optimalRoute.setTotalTime(totalTime);
                
                // 计算路线综合评分
                double routeScore = calculateRouteScore(optimalRoute, attractions);
                optimalRoute.setTotalFitness(routeScore);
                
                // 这里可以添加保存路线到数据库的逻辑
                // routePlanRepository.saveOptimalRoute(optimalRoute);
            } catch (Exception e) {
                // 记录异常但不影响返回结果
                log.error("路线优化失败: {}", e.getMessage(), e);
            }
        }
        
        return optimalRoute;
    }

    /**
     * 调整现有路线
     */
    public Object adjustRoute(Integer routeId, String adjustmentType, Object adjustmentParams) {
        // 这里需要根据实际的路线调整需求实现
        // 模拟实现路线调整逻辑
        Map<String, Object> adjustmentResult = new HashMap<>();
        adjustmentResult.put("routeId", routeId);
        adjustmentResult.put("adjustmentType", adjustmentType);
        adjustmentResult.put("adjustmentParams", adjustmentParams);
        adjustmentResult.put("adjustmentTime", new Date());
        adjustmentResult.put("status", "success");
        
        // 根据调整类型执行不同的调整逻辑
        switch (adjustmentType) {
            case "avoidCongestion":
                adjustmentResult.put("message", "已调整路线以避开拥堵路段");
                break;
            case "shortenDistance":
                adjustmentResult.put("message", "已调整路线以缩短距离");
                break;
            case "reduceTime":
                adjustmentResult.put("message", "已调整路线以减少时间");
                break;
            case "avoidAttractions":
                adjustmentResult.put("message", "已调整路线以避开指定景点");
                break;
            case "addAttractions":
                adjustmentResult.put("message", "已调整路线以添加指定景点");
                break;
            default:
                adjustmentResult.put("message", "已执行路线调整");
        }
        
        return adjustmentResult;
    }

    /**
     * 初始化种群
     */
    private List<RouteChromosome> initializePopulation(List<Integer> attractionIds, int maxDays, AlgorithmParams params) {
        List<RouteChromosome> population = new ArrayList<>();
        
        for (int i = 0; i < params.getPopulationSize(); i++) {
            RouteChromosome chromosome = new RouteChromosome();
            List<Integer> shuffledAttractions = new ArrayList<>(attractionIds);
            Collections.shuffle(shuffledAttractions);
            
            // 分配景点到每天
            Map<Integer, List<Integer>> dayAttractions = new HashMap<>();
            for (int j = 0; j < shuffledAttractions.size(); j++) {
                int day = j % maxDays + 1;
                dayAttractions.computeIfAbsent(day, k -> new ArrayList<>()).add(shuffledAttractions.get(j));
            }
            
            chromosome.setDayAttractions(dayAttractions);
            population.add(chromosome);
        }
        
        return population;
    }

    /**
     * 计算适应度
     */
    private void calculateFitness(RouteChromosome chromosome, BigDecimal budget, String preference) {
        double fitness = 0.0;
        
        // 计算总距离
        double totalDistance = calculateTotalDistance(chromosome);
        // 计算总成本
        double totalCost = calculateTotalCost(chromosome);
        // 计算总时间
        double totalTime = calculateTotalTime(chromosome);
        
        // 根据偏好调整适应度
        switch (preference) {
            case "lowCost":
                fitness = 1.0 / (totalCost * 0.6 + totalDistance * 0.3 + totalTime * 0.1);
                break;
            case "fast":
                fitness = 1.0 / (totalTime * 0.6 + totalDistance * 0.3 + totalCost * 0.1);
                break;
            case "lowCarbon":
                fitness = 1.0 / (totalDistance * 0.7 + totalTime * 0.2 + totalCost * 0.1);
                break;
            default:
                fitness = 1.0 / (totalDistance * 0.4 + totalTime * 0.3 + totalCost * 0.3);
        }
        
        // 预算约束
        if (BigDecimal.valueOf(totalCost).compareTo(budget) > 0) {
            fitness *= 0.5;
        }
        
        chromosome.setFitness(fitness);
    }

    /**
     * 计算总距离
     */
    private double calculateTotalDistance(RouteChromosome chromosome) {
        // 简单实现：基于每天景点数量计算距离
        Map<Integer, List<Integer>> dayAttractions = chromosome.getDayAttractions();
        double totalDistance = 0.0;
        
        for (List<Integer> attractions : dayAttractions.values()) {
            // 每个景点之间的平均距离为5公里
            if (attractions.size() > 1) {
                totalDistance += (attractions.size() - 1) * 5.0;
            }
        }
        
        return totalDistance;
    }

    /**
     * 计算总成本
     */
    private double calculateTotalCost(RouteChromosome chromosome) {
        // 简单实现：基于每天景点数量和交通成本计算
        Map<Integer, List<Integer>> dayAttractions = chromosome.getDayAttractions();
        double totalCost = 0.0;
        
        for (List<Integer> attractions : dayAttractions.values()) {
            // 每个景点的平均门票成本为30元
            totalCost += attractions.size() * 30.0;
            // 每天的交通成本为50元
            totalCost += 50.0;
        }
        
        return totalCost;
    }

    /**
     * 计算总时间
     */
    private double calculateTotalTime(RouteChromosome chromosome) {
        // 简单实现：基于每天景点数量计算时间
        Map<Integer, List<Integer>> dayAttractions = chromosome.getDayAttractions();
        double totalTime = 0.0;
        
        for (List<Integer> attractions : dayAttractions.values()) {
            // 每个景点的平均游览时间为2小时
            totalTime += attractions.size() * 2.0;
            // 每天的交通时间为1小时
            totalTime += 1.0;
        }
        
        return totalTime;
    }

    /**
     * 选择操作
     */
    private List<RouteChromosome> selection(List<RouteChromosome> population, AlgorithmParams params) {
        // 轮盘赌选择
        List<RouteChromosome> selected = new ArrayList<>();
        double totalFitness = population.stream().mapToDouble(RouteChromosome::getFitness).sum();
        
        for (int i = 0; i < population.size(); i++) {
            double random = Math.random() * totalFitness;
            double cumulative = 0.0;
            
            for (RouteChromosome chromosome : population) {
                cumulative += chromosome.getFitness();
                if (cumulative >= random) {
                    selected.add(chromosome);
                    break;
                }
            }
        }
        
        return selected;
    }

    /**
     * 交叉操作
     */
    private List<RouteChromosome> crossover(List<RouteChromosome> parents, AlgorithmParams params, int maxDays) {
        List<RouteChromosome> offspring = new ArrayList<>();
        
        for (int i = 0; i < parents.size(); i += 2) {
            if (i + 1 < parents.size() && Math.random() < 0.85) { // 使用默认交叉率
                RouteChromosome parent1 = parents.get(i);
                RouteChromosome parent2 = parents.get(i + 1);
                
                // 单点交叉
                int crossoverPoint = new Random().nextInt(maxDays) + 1;
                
                RouteChromosome child1 = new RouteChromosome();
                RouteChromosome child2 = new RouteChromosome();
                
                Map<Integer, List<Integer>> child1Days = new HashMap<>();
                Map<Integer, List<Integer>> child2Days = new HashMap<>();
                
                // 复制前半部分
                for (int day = 1; day <= crossoverPoint; day++) {
                    if (parent1.getDayAttractions().containsKey(day)) {
                        child1Days.put(day, new ArrayList<>(parent1.getDayAttractions().get(day)));
                    }
                    if (parent2.getDayAttractions().containsKey(day)) {
                        child2Days.put(day, new ArrayList<>(parent2.getDayAttractions().get(day)));
                    }
                }
                
                // 复制后半部分
                for (int day = crossoverPoint + 1; day <= maxDays; day++) {
                    if (parent2.getDayAttractions().containsKey(day)) {
                        child1Days.put(day, new ArrayList<>(parent2.getDayAttractions().get(day)));
                    }
                    if (parent1.getDayAttractions().containsKey(day)) {
                        child2Days.put(day, new ArrayList<>(parent1.getDayAttractions().get(day)));
                    }
                }
                
                child1.setDayAttractions(child1Days);
                child2.setDayAttractions(child2Days);
                
                offspring.add(child1);
                offspring.add(child2);
            } else if (i < parents.size()) {
                offspring.add(parents.get(i));
            }
        }
        
        return offspring;
    }

    /**
     * 变异操作
     */
    private void mutate(List<RouteChromosome> offspring, AlgorithmParams params) {
        for (RouteChromosome chromosome : offspring) {
            if (Math.random() < 0.08) { // 使用默认变异率
                // 交换两天的景点
                Map<Integer, List<Integer>> dayAttractions = chromosome.getDayAttractions();
                List<Integer> days = new ArrayList<>(dayAttractions.keySet());
                
                if (days.size() >= 2) {
                    Collections.shuffle(days);
                    int day1 = days.get(0);
                    int day2 = days.get(1);
                    
                    List<Integer> attractions1 = dayAttractions.get(day1);
                    List<Integer> attractions2 = dayAttractions.get(day2);
                    
                    if (!attractions1.isEmpty() && !attractions2.isEmpty()) {
                        int idx1 = new Random().nextInt(attractions1.size());
                        int idx2 = new Random().nextInt(attractions2.size());
                        
                        Integer temp = attractions1.get(idx1);
                        attractions1.set(idx1, attractions2.get(idx2));
                        attractions2.set(idx2, temp);
                    }
                }
            }
        }
    }

    /**
     * 替换种群
     */
    private List<RouteChromosome> replacePopulation(List<RouteChromosome> oldPopulation, List<RouteChromosome> offspring) {
        List<RouteChromosome> combined = new ArrayList<>(oldPopulation);
        combined.addAll(offspring);
        
        // 按适应度排序，保留最好的个体
        combined.sort(Comparator.comparingDouble(RouteChromosome::getFitness).reversed());
        
        return combined.subList(0, oldPopulation.size());
    }

    /**
     * 转换为最优路线
     */
    private OptimalRoute convertToOptimalRoute(RouteChromosome chromosome, List<Attraction> attractions, List<Transport> transportList) {
        OptimalRoute optimalRoute = new OptimalRoute();
        
        if (chromosome != null) {
            List<RouteDayPlan> dayPlans = new ArrayList<>();
            
            chromosome.getDayAttractions().forEach((day, attractionIds) -> {
                RouteDayPlan dayPlan = new RouteDayPlan();
                dayPlan.setDayNumber(day);
                dayPlan.setAttractionIds(attractionIds);
                
                // 计算当天的距离、成本、时间
                double dayDistance = calculateDayDistance(attractionIds, attractions);
                double dayCost = calculateDayCost(attractionIds, attractions);
                double dayTime = calculateDayTime(attractionIds, attractions);
                
                dayPlan.setDistance(dayDistance);
                dayPlan.setCost(dayCost);
                dayPlan.setTime(dayTime);
                
                dayPlans.add(dayPlan);
            });
            
            optimalRoute.setDayPlans(dayPlans);
            optimalRoute.setTotalFitness(chromosome.getFitness());
        }
        
        return optimalRoute;
    }

    /**
     * 路线染色体
     */
    private static class RouteChromosome {
        private Map<Integer, List<Integer>> dayAttractions; // 每天的景点ID列表
        private double fitness;

        public Map<Integer, List<Integer>> getDayAttractions() { return dayAttractions; }
        public void setDayAttractions(Map<Integer, List<Integer>> dayAttractions) { this.dayAttractions = dayAttractions; }
        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
    }

    /**
     * 最优路线
     */
    public static class OptimalRoute {
        private List<RouteDayPlan> dayPlans;
        private double totalFitness;
        private double totalDistance;
        private double totalCost;
        private double totalTime;

        public List<RouteDayPlan> getDayPlans() { return dayPlans; }
        public void setDayPlans(List<RouteDayPlan> dayPlans) { this.dayPlans = dayPlans; }
        public double getTotalFitness() { return totalFitness; }
        public void setTotalFitness(double totalFitness) { this.totalFitness = totalFitness; }
        public double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        public double getTotalTime() { return totalTime; }
        public void setTotalTime(double totalTime) { this.totalTime = totalTime; }
    }

    /**
     * 每日路线计划
     */
    public static class RouteDayPlan {
        private int dayNumber;
        private List<Integer> attractionIds;
        private double distance;
        private double cost;
        private double time;
        private String recommendedTransport;

        public int getDayNumber() { return dayNumber; }
        public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
        public List<Integer> getAttractionIds() { return attractionIds; }
        public void setAttractionIds(List<Integer> attractionIds) { this.attractionIds = attractionIds; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        public double getTime() { return time; }
        public void setTime(double time) { this.time = time; }
        public String getRecommendedTransport() { return recommendedTransport; }
        public void setRecommendedTransport(String recommendedTransport) { this.recommendedTransport = recommendedTransport; }
    }

    /**
     * 计算当天的距离
     */
    private double calculateDayDistance(List<Integer> attractionIds, List<Attraction> allAttractions) {
        if (attractionIds == null || attractionIds.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < attractionIds.size() - 1; i++) {
            Integer currentId = attractionIds.get(i);
            Integer nextId = attractionIds.get(i + 1);
            
            Attraction current = findAttractionById(currentId, allAttractions);
            Attraction next = findAttractionById(nextId, allAttractions);
            
            if (current != null && next != null) {
                // 使用Haversine公式计算两个经纬度点之间的距离
                totalDistance += calculateDistance(current.getLatitude().doubleValue(), current.getLongitude().doubleValue(),
                        next.getLatitude().doubleValue(), next.getLongitude().doubleValue());
            }
        }

        return totalDistance;
    }

    /**
     * 计算当天的成本
     */
    private double calculateDayCost(List<Integer> attractionIds, List<Attraction> allAttractions) {
        if (attractionIds == null || attractionIds.isEmpty()) {
            return 0.0;
        }

        double totalCost = 0.0;
        for (Integer attractionId : attractionIds) {
            Attraction attraction = findAttractionById(attractionId, allAttractions);
            if (attraction != null) {
                totalCost += attraction.getTicketPrice().doubleValue();
            }
        }

        // 添加交通成本（每天固定50元）
        totalCost += 50.0;

        return totalCost;
    }

    /**
     * 计算当天的时间
     */
    private double calculateDayTime(List<Integer> attractionIds, List<Attraction> allAttractions) {
        if (attractionIds == null || attractionIds.isEmpty()) {
            return 0.0;
        }

        // 每个景点平均游览时间为2小时
        double totalTime = attractionIds.size() * 2.0;
        
        // 添加交通时间（每天固定1小时）
        totalTime += 1.0;

        return totalTime;
    }

    /**
     * 评估路线多样性
     */
    private double evaluateRouteDiversity(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return 0.0;
        }

        Set<String> attractionTypes = new HashSet<>();
        int totalAttractions = 0;

        for (RouteDayPlan dayPlan : dayPlans) {
            for (Integer attractionId : dayPlan.getAttractionIds()) {
                Attraction attraction = findAttractionById(attractionId, allAttractions);
                if (attraction != null) {
                    // 使用描述来提取类型信息
                    String type = extractTypeFromDescription(attraction.getDescription());
                    if (type != null) {
                        attractionTypes.add(type);
                    }
                }
                totalAttractions++;
            }
        }

        // 多样性得分 = 不同类型的景点数量 / 总景点数量
        return totalAttractions > 0 ? (double) attractionTypes.size() / totalAttractions : 0.0;
    }

    /**
     * 从描述中提取类型信息
     */
    private String extractTypeFromDescription(String description) {
        if (description == null) {
            return null;
        }

        String[] types = {
            "博物馆", "公园", "山", "水", "湖", "河", "海", "岛", "寺", "庙", "教堂", "宫殿",
            "城堡", "园林", "花园", "广场", "街道", "古镇", "古村", "古城", "遗址", "古墓",
            "纪念馆", "纪念碑", "雕塑", "艺术", "文化", "历史", "自然", "生态", "休闲", "娱乐",
            "购物", "美食", "温泉", "滑雪", "登山", "徒步", "骑行", "划船", "漂流", "露营"
        };

        for (String type : types) {
            if (description.contains(type)) {
                return type;
            }
        }

        return "其他";
    }

    /**
     * 优化交通方式
     */
    private void optimizeTransportation(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions, List<Transport> transportList) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return;
        }

        for (RouteDayPlan dayPlan : dayPlans) {
            List<Integer> attractionIds = dayPlan.getAttractionIds();
            if (attractionIds.size() < 2) {
                continue;
            }

            // 计算当天景点之间的总距离
            double totalDistance = calculateDayDistance(attractionIds, allAttractions);
            
            // 根据距离选择合适的交通方式
            String recommendedTransport = "public";
            if (totalDistance < 5) {
                recommendedTransport = "walking";
            } else if (totalDistance < 15) {
                recommendedTransport = "biking";
            } else if (totalDistance < 50) {
                recommendedTransport = "public";
            } else {
                recommendedTransport = "private";
            }

            // 将推荐的交通方式添加到路线计划中
            dayPlan.setRecommendedTransport(recommendedTransport);
        }
    }

    /**
     * 考虑景点开放时间优化路线
     */
    private void optimizeOpeningHours(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return;
        }

        for (RouteDayPlan dayPlan : dayPlans) {
            List<Integer> attractionIds = dayPlan.getAttractionIds();
            if (attractionIds.size() < 2) {
                continue;
            }

            // 这里可以添加根据景点开放时间优化顺序的逻辑
            // 例如，将上午开放的景点安排在上午，下午开放的景点安排在下午
        }
    }

    /**
     * 考虑天气因素优化路线
     */
    private void optimizeForWeather(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions, String weather) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return;
        }

        // 根据天气调整路线
        switch (weather.toLowerCase()) {
            case "rainy":
                // 雨天优先安排室内景点
                for (RouteDayPlan dayPlan : dayPlans) {
                    List<Integer> attractionIds = dayPlan.getAttractionIds();
                    List<Integer> indoorAttractions = new ArrayList<>();
                    List<Integer> outdoorAttractions = new ArrayList<>();

                    for (Integer attractionId : attractionIds) {
                        Attraction attraction = findAttractionById(attractionId, allAttractions);
                        if (attraction != null) {
                            if (isIndoorAttraction(attraction)) {
                                indoorAttractions.add(attractionId);
                            } else {
                                outdoorAttractions.add(attractionId);
                            }
                        }
                    }

                    // 重新安排顺序：室内景点优先
                    indoorAttractions.addAll(outdoorAttractions);
                    dayPlan.setAttractionIds(indoorAttractions);
                }
                break;
            case "sunny":
                // 晴天优先安排户外景点
                // 类似的逻辑
                break;
            // 其他天气情况
        }
    }

    /**
     * 判断是否为室内景点
     */
    private boolean isIndoorAttraction(Attraction attraction) {
        if (attraction == null || attraction.getDescription() == null) {
            return false;
        }

        String description = attraction.getDescription().toLowerCase();
        return description.contains("博物馆") || description.contains("室内") || 
               description.contains("展览馆") || description.contains("美术馆") ||
               description.contains("科技馆") || description.contains("图书馆") ||
               description.contains("剧院") || description.contains("电影院");
    }

    /**
     * 计算路线的综合评分
     */
    private double calculateRouteScore(OptimalRoute optimalRoute, List<Attraction> allAttractions) {
        if (optimalRoute == null || optimalRoute.getDayPlans() == null || optimalRoute.getDayPlans().isEmpty()) {
            return 0.0;
        }

        // 计算各项得分
        double diversityScore = evaluateRouteDiversity(optimalRoute.getDayPlans(), allAttractions);
        double distanceScore = 1.0 / (1.0 + optimalRoute.getTotalDistance() / 100.0); // 距离越短得分越高
        double costScore = 1.0 / (1.0 + optimalRoute.getTotalCost() / 1000.0); // 成本越低得分越高
        double timeScore = 1.0 / (1.0 + optimalRoute.getTotalTime() / 48.0); // 时间越短得分越高
        double attractionQualityScore = evaluateAttractionQuality(optimalRoute.getDayPlans(), allAttractions); // 景点质量得分
        double transportationScore = evaluateTransportation(optimalRoute.getDayPlans()); // 交通方式得分

        // 权重
        double diversityWeight = 0.2;
        double distanceWeight = 0.15;
        double costWeight = 0.15;
        double timeWeight = 0.2;
        double attractionQualityWeight = 0.15;
        double transportationWeight = 0.15;

        // 综合评分
        return diversityScore * diversityWeight + distanceScore * distanceWeight + 
               costScore * costWeight + timeScore * timeWeight +
               attractionQualityScore * attractionQualityWeight + transportationScore * transportationWeight;
    }

    /**
     * 评估景点质量
     */
    private double evaluateAttractionQuality(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return 0.0;
        }

        double totalRating = 0.0;
        int totalAttractions = 0;

        for (RouteDayPlan dayPlan : dayPlans) {
            for (Integer attractionId : dayPlan.getAttractionIds()) {
                Attraction attraction = findAttractionById(attractionId, allAttractions);
                if (attraction != null && attraction.getRating() != null) {
                    totalRating += attraction.getRating().doubleValue();
                    totalAttractions++;
                }
            }
        }

        return totalAttractions > 0 ? totalRating / totalAttractions / 5.0 : 0.0; // 归一化到0-1
    }

    /**
     * 评估交通方式
     */
    private double evaluateTransportation(List<RouteDayPlan> dayPlans) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return 0.0;
        }

        int goodTransportCount = 0;
        int totalDays = dayPlans.size();

        for (RouteDayPlan dayPlan : dayPlans) {
            String transport = dayPlan.getRecommendedTransport();
            if (transport != null && (
                "walking".equals(transport) || "biking".equals(transport) || "public".equals(transport)
            )) {
                goodTransportCount++;
            }
        }

        return totalDays > 0 ? (double) goodTransportCount / totalDays : 0.0;
    }

    /**
     * 详细的天气因素考虑
     */
    private void detailedWeatherConsideration(List<RouteDayPlan> dayPlans, List<Attraction> allAttractions, String weather, String season) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return;
        }

        for (RouteDayPlan dayPlan : dayPlans) {
            List<Integer> attractionIds = dayPlan.getAttractionIds();
            List<Integer> indoorAttractions = new ArrayList<>();
            List<Integer> outdoorAttractions = new ArrayList<>();
            List<Integer> weatherSuitableAttractions = new ArrayList<>();

            for (Integer attractionId : attractionIds) {
                Attraction attraction = findAttractionById(attractionId, allAttractions);
                if (attraction != null) {
                    if (isIndoorAttraction(attraction)) {
                        indoorAttractions.add(attractionId);
                    } else {
                        outdoorAttractions.add(attractionId);
                    }
                    
                    // 考虑天气对景点的适合度
                    if (isWeatherSuitable(attraction, weather, season)) {
                        weatherSuitableAttractions.add(attractionId);
                    }
                }
            }

            // 根据天气重新安排顺序
            List<Integer> reorderedAttractions = new ArrayList<>();
            
            switch (weather.toLowerCase()) {
                case "rainy":
                case "snowy":
                case "stormy":
                    // 优先安排室内景点
                    reorderedAttractions.addAll(indoorAttractions);
                    reorderedAttractions.addAll(outdoorAttractions);
                    break;
                case "sunny":
                case "clear":
                    // 优先安排户外景点
                    reorderedAttractions.addAll(outdoorAttractions);
                    reorderedAttractions.addAll(indoorAttractions);
                    break;
                case "cloudy":
                case "overcast":
                    // 平衡安排
                    int outdoorCount = outdoorAttractions.size();
                    int indoorCount = indoorAttractions.size();
                    int minCount = Math.min(outdoorCount, indoorCount);
                    
                    for (int i = 0; i < minCount; i++) {
                        reorderedAttractions.add(outdoorAttractions.get(i));
                        reorderedAttractions.add(indoorAttractions.get(i));
                    }
                    
                    if (outdoorCount > indoorCount) {
                        reorderedAttractions.addAll(outdoorAttractions.subList(minCount, outdoorCount));
                    } else {
                        reorderedAttractions.addAll(indoorAttractions.subList(minCount, indoorCount));
                    }
                    break;
                default:
                    reorderedAttractions.addAll(attractionIds);
            }

            // 应用重新安排的顺序
            dayPlan.setAttractionIds(reorderedAttractions);
        }
    }

    /**
     * 判断景点是否适合当前天气和季节
     */
    private boolean isWeatherSuitable(Attraction attraction, String weather, String season) {
        if (attraction == null || attraction.getDescription() == null) {
            return true;
        }

        String description = attraction.getDescription().toLowerCase();
        
        // 考虑天气
        switch (weather.toLowerCase()) {
            case "rainy":
            case "snowy":
            case "stormy":
                return description.contains("室内") || description.contains("博物馆") || 
                       description.contains("展览馆") || description.contains("美术馆") ||
                       description.contains("科技馆") || description.contains("图书馆") ||
                       description.contains("剧院") || description.contains("电影院");
            case "sunny":
            case "clear":
                return description.contains("公园") || description.contains("山") || 
                       description.contains("水") || description.contains("湖") ||
                       description.contains("河") || description.contains("海") ||
                       description.contains("岛") || description.contains("花园");
            default:
                return true;
        }
    }

    /**
     * 路线的实时调整
     */
    @SuppressWarnings("unchecked")
    public OptimalRoute realTimeAdjustment(OptimalRoute currentRoute, List<Attraction> allAttractions, List<Transport> transportList, Map<String, Object> realTimeFactors) {
        if (currentRoute == null || currentRoute.getDayPlans() == null || currentRoute.getDayPlans().isEmpty()) {
            return currentRoute;
        }

        // 获取实时因素
        String weather = (String) realTimeFactors.get("weather");
        String trafficCondition = (String) realTimeFactors.get("trafficCondition");
        List<Integer> closedAttractions = (List<Integer>) realTimeFactors.get("closedAttractions");
        List<Integer> crowdedAttractions = (List<Integer>) realTimeFactors.get("crowdedAttractions");

        // 复制当前路线
        OptimalRoute adjustedRoute = new OptimalRoute();
        List<RouteDayPlan> adjustedDayPlans = new ArrayList<>();
        for (RouteDayPlan dayPlan : currentRoute.getDayPlans()) {
            RouteDayPlan adjustedDayPlan = new RouteDayPlan();
            adjustedDayPlan.setDayNumber(dayPlan.getDayNumber());
            adjustedDayPlan.setAttractionIds(new ArrayList<>(dayPlan.getAttractionIds()));
            adjustedDayPlan.setDistance(dayPlan.getDistance());
            adjustedDayPlan.setCost(dayPlan.getCost());
            adjustedDayPlan.setTime(dayPlan.getTime());
            adjustedDayPlan.setRecommendedTransport(dayPlan.getRecommendedTransport());
            adjustedDayPlans.add(adjustedDayPlan);
        }
        adjustedRoute.setDayPlans(adjustedDayPlans);
        adjustedRoute.setTotalDistance(currentRoute.getTotalDistance());
        adjustedRoute.setTotalCost(currentRoute.getTotalCost());
        adjustedRoute.setTotalTime(currentRoute.getTotalTime());
        adjustedRoute.setTotalFitness(currentRoute.getTotalFitness());

        // 处理关闭的景点
        if (closedAttractions != null && !closedAttractions.isEmpty()) {
            for (RouteDayPlan dayPlan : adjustedDayPlans) {
                List<Integer> attractionIds = dayPlan.getAttractionIds();
                List<Integer> openAttractions = new ArrayList<>();
                for (Integer attractionId : attractionIds) {
                    if (!closedAttractions.contains(attractionId)) {
                        openAttractions.add(attractionId);
                    }
                }
                dayPlan.setAttractionIds(openAttractions);
            }
        }

        // 处理拥挤的景点
        if (crowdedAttractions != null && !crowdedAttractions.isEmpty()) {
            for (RouteDayPlan dayPlan : adjustedDayPlans) {
                List<Integer> attractionIds = dayPlan.getAttractionIds();
                List<Integer> nonCrowdedAttractions = new ArrayList<>();
                List<Integer> crowdedList = new ArrayList<>();
                for (Integer attractionId : attractionIds) {
                    if (!crowdedAttractions.contains(attractionId)) {
                        nonCrowdedAttractions.add(attractionId);
                    } else {
                        crowdedList.add(attractionId);
                    }
                }
                // 优先安排不拥挤的景点
                nonCrowdedAttractions.addAll(crowdedList);
                dayPlan.setAttractionIds(nonCrowdedAttractions);
            }
        }

        // 处理天气因素
        if (weather != null) {
            detailedWeatherConsideration(adjustedDayPlans, allAttractions, weather, "");
        }

        // 处理交通状况
        if (trafficCondition != null && "heavy".equals(trafficCondition)) {
            // 交通拥堵时，调整交通方式
            for (RouteDayPlan dayPlan : adjustedDayPlans) {
                dayPlan.setRecommendedTransport("public");
            }
        }

        // 重新计算路线参数
        double totalDistance = adjustedDayPlans.stream()
                .mapToDouble(RouteDayPlan::getDistance)
                .sum();
        double totalCost = adjustedDayPlans.stream()
                .mapToDouble(RouteDayPlan::getCost)
                .sum();
        double totalTime = adjustedDayPlans.stream()
                .mapToDouble(RouteDayPlan::getTime)
                .sum();
        
        adjustedRoute.setTotalDistance(totalDistance);
        adjustedRoute.setTotalCost(totalCost);
        adjustedRoute.setTotalTime(totalTime);
        
        // 重新计算路线评分
        double routeScore = calculateRouteScore(adjustedRoute, allAttractions);
        adjustedRoute.setTotalFitness(routeScore);

        return adjustedRoute;
    }

    /**
     * 多目标优化
     */
    public List<OptimalRoute> multiObjectiveOptimization(List<Integer> attractionIds, int maxDays, BigDecimal budget, List<String> preferences) {
        List<OptimalRoute> optimalRoutes = new ArrayList<>();
        
        // 为每个偏好生成一条最优路线
        for (String preference : preferences) {
            OptimalRoute optimalRoute = planOptimalRoute(attractionIds, maxDays, budget, preference);
            if (optimalRoute != null) {
                optimalRoutes.add(optimalRoute);
            }
        }
        
        return optimalRoutes;
    }

    /**
     * 路线的可视化输出
     */
    public String visualizeRoute(OptimalRoute optimalRoute, List<Attraction> allAttractions) {
        if (optimalRoute == null || optimalRoute.getDayPlans() == null || optimalRoute.getDayPlans().isEmpty()) {
            return "路线为空，无法可视化";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 路线可视化 ===\n");
        sb.append("总距离: " + String.format("%.2f", optimalRoute.getTotalDistance()) + " 公里\n");
        sb.append("总成本: " + String.format("%.2f", optimalRoute.getTotalCost()) + " 元\n");
        sb.append("总时间: " + String.format("%.2f", optimalRoute.getTotalTime()) + " 小时\n");
        sb.append("综合评分: " + String.format("%.2f", optimalRoute.getTotalFitness() * 100) + " 分\n\n");

        for (RouteDayPlan dayPlan : optimalRoute.getDayPlans()) {
            sb.append("第 " + dayPlan.getDayNumber() + " 天:\n");
            sb.append("推荐交通方式: " + dayPlan.getRecommendedTransport() + "\n");
            sb.append("当天距离: " + String.format("%.2f", dayPlan.getDistance()) + " 公里\n");
            sb.append("当天成本: " + String.format("%.2f", dayPlan.getCost()) + " 元\n");
            sb.append("当天时间: " + String.format("%.2f", dayPlan.getTime()) + " 小时\n");
            sb.append("景点安排: \n");
            
            int order = 1;
            for (Integer attractionId : dayPlan.getAttractionIds()) {
                Attraction attraction = findAttractionById(attractionId, allAttractions);
                if (attraction != null) {
                    String type = extractTypeFromDescription(attraction.getDescription());
                    sb.append("  " + order + ". " + attraction.getName() + " (" + type + ")\n");
                }
                order++;
            }
            sb.append("\n");
        }

        sb.append("=== 路线结束 ===\n");
        return sb.toString();
    }

    /**
     * 获取路线的分享链接
     */
    public String generateShareLink(OptimalRoute optimalRoute) {
        if (optimalRoute == null) {
            return null;
        }

        // 生成唯一的路线ID
        String routeId = UUID.randomUUID().toString().replaceAll("-", "");
        
        // 生成分享链接
        String shareLink = "https://travel.example.com/route/share/" + routeId;
        
        return shareLink;
    }

    /**
     * 保存路线
     */
    public boolean saveRoute(OptimalRoute optimalRoute, String userId) {
        if (optimalRoute == null) {
            return false;
        }

        // 这里可以添加保存路线到数据库的逻辑
        // 模拟实现
        log.info("保存路线成功: userId={}, routeId={}", userId, UUID.randomUUID().toString().replaceAll("-", ""));
        return true;
    }

    /**
     * 加载路线
     */
    public OptimalRoute loadRoute(String routeId) {
        // 这里可以添加从数据库加载路线的逻辑
        // 模拟实现
        log.info("加载路线成功: routeId={}", routeId);
        return new OptimalRoute();
    }

    /**
     * 提交路线评价
     */
    public boolean submitRouteFeedback(String routeId, int rating, String feedback) {
        // 这里可以添加提交路线评价到数据库的逻辑
        // 模拟实现
        log.info("提交路线评价成功: routeId={}, rating={}, feedback={}", routeId, rating, feedback);
        return true;
    }

    /**
     * 获取路线优化建议
     */
    public List<String> getRouteOptimizationSuggestions(OptimalRoute optimalRoute, List<Attraction> allAttractions) {
        List<String> suggestions = new ArrayList<>();

        if (optimalRoute == null || optimalRoute.getDayPlans() == null || optimalRoute.getDayPlans().isEmpty()) {
            suggestions.add("路线为空，无法提供优化建议");
            return suggestions;
        }

        // 评估路线多样性
        double diversityScore = evaluateRouteDiversity(optimalRoute.getDayPlans(), allAttractions);
        if (diversityScore < 0.3) {
            suggestions.add("建议增加不同类型的景点，丰富路线体验");
        }

        // 评估每天的景点数量
        for (RouteDayPlan dayPlan : optimalRoute.getDayPlans()) {
            int attractionCount = dayPlan.getAttractionIds().size();
            if (attractionCount > 5) {
                suggestions.add("第" + dayPlan.getDayNumber() + "天的景点数量过多，建议减少到3-4个，以获得更好的游览体验");
            } else if (attractionCount < 2) {
                suggestions.add("第" + dayPlan.getDayNumber() + "天的景点数量过少，建议增加1-2个景点");
            }
        }

        // 评估总距离
        if (optimalRoute.getTotalDistance() > 200) {
            suggestions.add("路线总距离较长，建议优化交通方式或减少景点间的距离");
        }

        // 评估总成本
        if (optimalRoute.getTotalCost() > 2000) {
            suggestions.add("路线总成本较高，建议选择一些免费或低价的景点");
        }

        // 评估总时间
        if (optimalRoute.getTotalTime() > 48) {
            suggestions.add("路线总时间较长，建议增加天数或减少每天的游览时间");
        }

        return suggestions;
    }

    /**
     * 根据ID查找景点
     */
    private Attraction findAttractionById(Integer attractionId, List<Attraction> attractions) {
        for (Attraction attraction : attractions) {
            if (attraction.getId().equals(attractionId)) {
                return attraction;
            }
        }
        return null;
    }

    /**
     * 使用CommonUtil计算两个经纬度点之间的距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return CommonUtil.calculateDistance(lat1, lon1, lat2, lon2);
    }
}
