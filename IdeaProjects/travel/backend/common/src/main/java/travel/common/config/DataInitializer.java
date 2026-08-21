package travel.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_recommendation.City;
import travel.common.entity.travel_recommendation.Restaurant;
import travel.common.entity.user_community.User;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.common.mapper.travel_recommendation_mapper.CityMapper;
import travel.common.mapper.travel_recommendation_mapper.RestaurantMapper;
import travel.common.mapper.user_community_mapper.UserMapper;
import travel.common.mapper.system_mapper.UiDictionaryMapper;
import travel.common.entity.system.UiDictionary;
import travel.common.utils.CommonUtil;
import travel.common.utils.PasswordEncoderUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 首次启动时自动插入测试数据，数据库已有数据时跳过
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String TEST_DATA_PASSWORD_ENV = "TEST_DATA_USER_PASSWORD";

    private final UserMapper userMapper;
    private final CityMapper cityMapper;
    private final AttractionMapper attractionMapper;
    private final RouteMapper routeMapper;
    private final RestaurantMapper restaurantMapper;
    private final UiDictionaryMapper dictionaryMapper;

    @Override
    public void run(String... args) {
        log.info("========== 开始检查数据库初始化数据 ==========");

        long userCount = userMapper.selectCount(null);
        long cityCount = cityMapper.selectCount(null);
        long attractionCount = attractionMapper.selectCount(null);
        long routeCount = routeMapper.selectCount(null);
        long restaurantCount = restaurantMapper.selectCount(null);

        log.info("当前数据: 用户={}, 城市={}, 景点={}, 路线={}, 餐厅={}", userCount, cityCount, attractionCount, routeCount, restaurantCount);

        if (cityCount == 0) seedCities();
        if (userCount == 0) seedUsers();
        if (attractionCount == 0) seedAttractions();
        if (routeCount == 0) seedRoutes();
        if (restaurantCount == 0) seedRestaurants();

        long dictCount = dictionaryMapper.selectCount(null);
        if (dictCount == 0) seedDictionary();

        log.info("========== 数据初始化完成 ==========");
    }

    private void seedCities() {
        log.info("初始化城市数据...");
        cityMapper.insert(city("南京", "中国", "江苏", 32.0603, 118.7969, "六朝古都，十朝都会"));
        cityMapper.insert(city("北京", "中国", "北京", 39.9042, 116.4074, "千年帝都，中华心脏"));
        cityMapper.insert(city("杭州", "中国", "浙江", 30.2741, 120.1551, "上有天堂，下有苏杭"));
        cityMapper.insert(city("成都", "中国", "四川", 30.5728, 104.0668, "天府之国，美食之都"));
        log.info("城市数据初始化完成: 4条");
    }

    private void seedUsers() {
        log.info("初始化测试用户...");
        String initialPassword = resolveSeedUserPassword();

        User user = new User();
        user.setUsername("testuser");
        user.setPhone("13800138000");
        user.setPassword(PasswordEncoderUtil.encode(initialPassword));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("测试用户创建成功: {}", user.getUsername());

        // 再创建一个用户用于路线数据
        User user2 = new User();
        user2.setUsername("demo");
        user2.setPhone("13900139000");
        user2.setPassword(PasswordEncoderUtil.encode(initialPassword));
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user2);
        log.info("测试用户创建成功: {}", user2.getUsername());

        if (System.getenv(TEST_DATA_PASSWORD_ENV) != null && !System.getenv(TEST_DATA_PASSWORD_ENV).isBlank()) {
            log.info("测试用户密码已通过环境变量 {} 提供", TEST_DATA_PASSWORD_ENV);
        } else {
            log.warn("未设置环境变量 {}，已为初始化测试用户生成随机密码", TEST_DATA_PASSWORD_ENV);
        }
    }

    private String resolveSeedUserPassword() {
        String configuredPassword = System.getenv(TEST_DATA_PASSWORD_ENV);
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return configuredPassword;
        }
        return CommonUtil.generateRandomString(16);
    }
    private void seedAttractions() {
        log.info("初始化景点数据...");
        // 南京景点
        attractionMapper.insert(attraction("中山陵", 1, "南京市玄武区中山门外石象路7号", "中国近代伟大的民主革命先行者孙中山先生的陵寝，国家5A级旅游景区", "免费", "08:30-17:00", new BigDecimal("4.8"), 32.0603, 118.7969, "https://youimg1.c-ctrip.com/target/100v1f000001h2q1p6B8E.jpg"));
        attractionMapper.insert(attraction("夫子庙", 1, "南京市秦淮区秦淮河北岸贡院街", "南京历史文化地标，秦淮风光带核心区域，集古迹、园林、画舫于一体", "30元", "09:00-22:00", new BigDecimal("4.5"), 32.0226, 118.7894, "https://youimg1.c-ctrip.com/target/100r1f000001h1qmiE7A8.jpg"));
        attractionMapper.insert(attraction("玄武湖", 1, "南京市玄武区玄武巷1号", "中国最大的皇家园林湖泊，江南三大名湖之一", "免费", "06:00-21:00", new BigDecimal("4.6"), 32.0709, 118.7980, "https://youimg1.c-ctrip.com/target/100o1f000001h0qpm200B.jpg"));
        attractionMapper.insert(attraction("明孝陵", 1, "南京市玄武区紫金山南麓", "明朝开国皇帝朱元璋与马皇后的合葬陵墓，世界文化遗产", "70元", "07:00-18:00", new BigDecimal("4.7"), 32.0583, 118.8347, "https://youimg1.c-ctrip.com/target/0104r12000a2q7p5j3B8D.jpg"));

        // 北京景点
        attractionMapper.insert(attraction("故宫博物院", 2, "北京市东城区景山前街4号", "明清两代皇家宫殿，世界最大木质结构建筑群，世界文化遗产", "60元", "08:30-17:00", new BigDecimal("4.9"), 39.9163, 116.3972, "https://youimg1.c-ctrip.com/target/100g1f000001h27q2A9E4.jpg"));
        attractionMapper.insert(attraction("长城(八达岭)", 2, "北京市延庆区G6京藏高速58号出口", "世界七大奇迹之一，中华民族的象征，世界文化遗产", "45元", "07:30-17:00", new BigDecimal("4.7"), 40.3597, 116.0204, "https://youimg1.c-ctrip.com/target/100r1f000001h1q8qE7F2.jpg"));
        attractionMapper.insert(attraction("颐和园", 2, "北京市海淀区新建宫门路19号", "中国现存规模最大、保存最完整的皇家园林，世界文化遗产", "30元", "07:00-18:00", new BigDecimal("4.8"), 39.9999, 116.2755, "https://youimg1.c-ctrip.com/target/100b1f000001h1qmiB3A8.jpg"));

        // 杭州景点
        attractionMapper.insert(attraction("西湖", 3, "杭州市西湖区西湖风景区", "中国十大风景名胜之一，世界文化遗产，入选世界遗产名录", "免费", "全天开放", new BigDecimal("4.8"), 30.2372, 120.1409, "https://youimg1.c-ctrip.com/target/100g1f000001h1q8qE7F2.jpg"));
        attractionMapper.insert(attraction("灵隐寺", 3, "杭州市西湖区法云弄1号", "中国佛教禅宗十大古刹之一，江南著名古刹", "45元", "07:00-18:00", new BigDecimal("4.6"), 30.2429, 120.1005, "https://youimg1.c-ctrip.com/target/100r1f000001h1qmiB3A8.jpg"));

        // 成都景点
        attractionMapper.insert(attraction("宽窄巷子", 4, "成都市青羊区长顺街附近", "成都最具代表性的历史文化街区，集美食、民俗、文创于一体", "免费", "全天开放", new BigDecimal("4.5"), 30.6678, 104.0550, "https://youimg1.c-ctrip.com/target/100o1f000001h0qpm200B.jpg"));
        attractionMapper.insert(attraction("大熊猫繁育基地", 4, "成都市成华区熊猫大道1375号", "全球最大的大熊猫圈养种群基地，近距离观察国宝", "58元", "07:30-18:00", new BigDecimal("4.8"), 30.7332, 104.1415, "https://youimg1.c-ctrip.com/target/100b1f000001h1qmiB3A8.jpg"));
        log.info("景点数据初始化完成: 11条");
    }

    private void seedRoutes() {
        log.info("初始化路线数据...");
        routeMapper.insert(route("南京经典一日游", "游中山陵缅怀伟人，访夫子庙品秦淮风韵，漫步玄武湖赏湖光山色", 1, 1, 1, "中等", "https://youimg1.c-ctrip.com/target/100v1f000001h2q1p6B8E.jpg"));
        routeMapper.insert(route("南京深度两日游", "Day1中山陵+明孝陵感受历史，Day2玄武湖+夫子庙体验文化", 1, 2, 1, "中等", "https://youimg1.c-ctrip.com/target/100r1f000001h1qmiE7A8.jpg"));
        routeMapper.insert(route("北京皇城根文化之旅", "故宫→天安门→颐和园，感受千年帝都的皇家气派", 2, 2, 2, "轻松", "https://youimg1.c-ctrip.com/target/100g1f000001h27q2A9E4.jpg"));
        routeMapper.insert(route("杭州西湖环湖漫步", "西湖十景一一打卡，灵隐寺祈福，品味江南韵味", 3, 1, 1, "轻松", "https://youimg1.c-ctrip.com/target/100g1f000001h1q8qE7F2.jpg"));
        routeMapper.insert(route("成都美食休闲之旅", "宽窄巷子逛吃，大熊猫基地看国宝，感受天府慢生活", 4, 2, 2, "轻松", "https://youimg1.c-ctrip.com/target/100o1f000001h0qpm200B.jpg"));
        log.info("路线数据初始化完成: 5条");
    }

    private void seedRestaurants() {
        log.info("初始化餐厅数据...");
        restaurantMapper.insert(restaurant("南京大牌档(夫子庙店)", 1, "南京市秦淮区大石坝街48号", 4.5, "中餐", "人均80元", "南京特色小吃，推荐盐水鸭、鸭血粉丝汤", "11:00-21:30"));
        restaurantMapper.insert(restaurant("全聚德烤鸭店(前门店)", 2, "北京市东城区前门大街30号", 4.6, "中餐", "人均150元", "北京烤鸭老字号，皮脆肉嫩", "11:00-21:00"));
        restaurantMapper.insert(restaurant("楼外楼(孤山路店)", 3, "杭州市西湖区孤山路30号", 4.4, "中餐", "人均120元", "西湖醋鱼、东坡肉，杭帮菜代表", "11:00-20:30"));
        restaurantMapper.insert(restaurant("小龙坎火锅(春熙路店)", 4, "成都市锦江区春熙路北段", 4.6, "火锅", "人均100元", "正宗成都火锅，麻辣鲜香", "11:00-02:00"));
        log.info("餐厅数据初始化完成: 4条");
    }

    // ==================== 辅助方法 ====================

    private City city(String name, String country, String province, double lat, double lng, String desc) {
        City c = new City();
        c.setName(name);
        c.setCountry(country);
        c.setProvince(province);
        c.setLatitude(new BigDecimal(lat));
        c.setLongitude(new BigDecimal(lng));
        c.setDescription(desc);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private Attraction attraction(String name, int cityId, String address, String desc, String ticket, String hours, BigDecimal rating, double lat, double lng, String images) {
        Attraction a = new Attraction();
        a.setName(name);
        a.setCityId(cityId);
        a.setAddress(address);
        a.setDescription(desc);
        a.setTicketPrice(new BigDecimal(ticket.replaceAll("[^0-9.]", "").isEmpty() ? "0" : ticket.replaceAll("[^0-9.]", "")));
        a.setOpeningHours(hours);
        a.setRating(rating);
        a.setLatitude(new BigDecimal(lat));
        a.setLongitude(new BigDecimal(lng));
        a.setImages(images);
        a.setViewCount(0);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return a;
    }

    private Route route(String title, String desc, int cityId, int duration, int userId, String difficulty, String coverImage) {
        Route r = new Route();
        r.setTitle(title);
        r.setDescription(desc);
        r.setCityId(cityId);
        r.setDurationDays(duration);
        r.setUserId(userId);
        r.setDifficulty(difficulty);
        r.setCoverImage(coverImage);
        r.setIsPublic(true);
        r.setViewCount(0);
        r.setLikeCount(0);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private Restaurant restaurant(String name, int cityId, String address, double rating, String cuisine, String cost, String desc, String hours) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCityId(cityId);
        r.setAddress(address);
        r.setRating(rating);
        r.setCuisineType(cuisine);
        r.setAverageCost(new BigDecimal(cost.replaceAll("[^0-9.]", "").isEmpty() ? "0" : cost.replaceAll("[^0-9.]", "")));
        r.setDescription(desc);
        r.setOpeningHours(hours);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private void seedDictionary() {
        log.info("初始化UI字典数据...");

        // 导航菜单
        insertDict("nav_menu", "/",           "/",           "首页",   1);
        insertDict("nav_menu", "/attractions","/attractions","景点",   2);
        insertDict("nav_menu", "/restaurants","/restaurants","美食",   3);
        insertDict("nav_menu", "/routes",     "/routes",     "路线",   4);
        insertDict("nav_menu", "/optimization","/optimization","优化", 5);
        insertDict("nav_menu", "/realtime",   "/realtime",   "实时",   6);
        insertDict("nav_menu", "/notes",      "/notes",      "游记",   7);
        insertDict("nav_menu", "/files",      "/files",      "文件",   8);
        insertDict("nav_menu", "/share",      "/share",      "分享",   9);
        insertDict("nav_menu", "/feedback",   "/feedback",   "反馈",   10);
        insertDict("nav_menu", "/ai-chat",    "/ai-chat",    "AI",     11);

        // 路线Tab
        insertDict("route_tabs", "all",      "all",      "全部路线", 1);
        insertDict("route_tabs", "popular",  "popular",  "热门路线", 2);
        insertDict("route_tabs", "smart",    "smart",    "智能推荐", 3);
        insertDict("route_tabs", "seasonal", "seasonal", "季节推荐", 4);
        insertDict("route_tabs", "theme",    "theme",    "主题路线", 5);

        log.info("UI字典数据初始化完成: {}条", dictionaryMapper.selectCount(null));
    }

    private void insertDict(String type, String key, String value, String label, int order) {
        UiDictionary d = new UiDictionary();
        d.setDictType(type);
        d.setDictKey(key);
        d.setDictValue(value);
        d.setDictLabel(label);
        d.setSortOrder(order);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        dictionaryMapper.insert(d);
    }
}

