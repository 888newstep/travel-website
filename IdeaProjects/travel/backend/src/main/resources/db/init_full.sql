-- 创建数据库
CREATE DATABASE IF NOT EXISTS travel_website 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE travel_website;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    avatar VARCHAR(200) COMMENT '头像',
    phone VARCHAR(20) COMMENT '手机号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) COMMENT='用户表' ENGINE=InnoDB;

-- 城市表
CREATE TABLE IF NOT EXISTS cities (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '城市ID',
    name VARCHAR(50) NOT NULL COMMENT '城市名称',
    country VARCHAR(50) COMMENT '国家',
    province VARCHAR(50) COMMENT '省份',
    latitude DECIMAL(10, 8) COMMENT '纬度',
    longitude DECIMAL(11, 8) COMMENT '经度',
    description TEXT COMMENT '城市简介',
    cover_image VARCHAR(200) COMMENT '封面图',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_country (country),
    INDEX idx_province (province)
) COMMENT='城市表' ENGINE=InnoDB;

-- 景点表
CREATE TABLE IF NOT EXISTS attractions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '景点ID',
    name VARCHAR(100) NOT NULL COMMENT '景点名称',
    city_id INT NOT NULL COMMENT '城市ID',
    address VARCHAR(200) COMMENT '地址',
    description TEXT COMMENT '景点描述',
    ticket_price DECIMAL(10, 2) DEFAULT 0 COMMENT '门票价格',
    opening_hours VARCHAR(100) COMMENT '开放时间',
    latitude DECIMAL(10, 8) COMMENT '纬度',
    longitude DECIMAL(11, 8) COMMENT '经度',
    images TEXT COMMENT '图片(JSON格式)',
    rating DECIMAL(3, 2) DEFAULT 0 COMMENT '评分',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    INDEX idx_city (city_id),
    INDEX idx_rating (rating DESC),
    INDEX idx_price (ticket_price)
) COMMENT='景点表' ENGINE=InnoDB;

-- 路线表
CREATE TABLE IF NOT EXISTS routes (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '路线ID',
    title VARCHAR(100) NOT NULL COMMENT '路线标题',
    description TEXT COMMENT '路线描述',
    city_id INT NOT NULL COMMENT '城市ID',
    duration_days INT DEFAULT 1 COMMENT '天数',
    difficulty VARCHAR(20) DEFAULT '中等' COMMENT '难度',
    cover_image VARCHAR(200) COMMENT '封面图',
    user_id INT COMMENT '创建用户ID',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_city (city_id),
    INDEX idx_user (user_id),
    INDEX idx_public (is_public),
    INDEX idx_views (view_count DESC)
) COMMENT='路线表' ENGINE=InnoDB;

-- 路线-景点关联表
CREATE TABLE IF NOT EXISTS route_attractions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    route_id INT NOT NULL COMMENT '路线ID',
    attraction_id INT NOT NULL COMMENT '景点ID',
    day_number INT DEFAULT 1 COMMENT '第几天',
    visit_order INT COMMENT '游览顺序',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (attraction_id) REFERENCES attractions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_route_attraction (route_id, attraction_id),
    INDEX idx_route (route_id),
    INDEX idx_attraction (attraction_id),
    INDEX idx_day (route_id, day_number)
) COMMENT='路线-景点关联表' ENGINE=InnoDB;

-- 交通工具表
CREATE TABLE IF NOT EXISTS transport (
    transport_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '交通工具ID',
    transport_name VARCHAR(100) NOT NULL COMMENT '交通工具名称',
    transport_type ENUM('walking', 'bus', 'subway', 'taxi', 'car', 'train', 'bicycle', 'boat', 'plane') NOT NULL COMMENT '交通工具类型',
    icon_url VARCHAR(500) COMMENT '图标URL',
    avg_speed_kmh DECIMAL(5, 2) COMMENT '平均速度(km/h)',
    cost_per_km DECIMAL(6, 2) COMMENT '每公里费用',
    co2_emission DECIMAL(6, 3) COMMENT '每公里碳排放(kg)',
    description TEXT COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (transport_type),
    INDEX idx_name (transport_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交通工具表';

-- 路线交通关联表
CREATE TABLE IF NOT EXISTS route_transport (
    route_transport_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '路线交通ID',
    route_id INT NOT NULL COMMENT '路线ID',
    from_attraction_id INT NOT NULL COMMENT '出发景点ID',
    to_attraction_id INT NOT NULL COMMENT '到达景点ID',
    transport_id INT NOT NULL COMMENT '交通工具ID',
    transport_order INT NOT NULL COMMENT '交通顺序',
    estimated_duration INT COMMENT '预计用时(分钟)',
    distance DECIMAL(6, 2) COMMENT '距离(公里)',
    instructions TEXT COMMENT '交通指示',
    cost_estimate DECIMAL(8, 2) COMMENT '费用预估',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (from_attraction_id) REFERENCES attractions(id) ON DELETE CASCADE,
    FOREIGN KEY (to_attraction_id) REFERENCES attractions(id) ON DELETE CASCADE,
    FOREIGN KEY (transport_id) REFERENCES transport(transport_id) ON DELETE CASCADE,
    UNIQUE KEY uk_route_transport_order (route_id, transport_order),
    INDEX idx_route (route_id),
    INDEX idx_transport (transport_id),
    INDEX idx_from_attraction (from_attraction_id),
    INDEX idx_to_attraction (to_attraction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线交通关联表';

-- 景点实时状态表
CREATE TABLE IF NOT EXISTS attraction_realtime_status (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    attraction_id INT NOT NULL COMMENT '景点ID',
    weather VARCHAR(50) COMMENT '天气',
    temperature INT COMMENT '温度',
    crowd_count INT COMMENT '实时人数',
    crowd_level INT COMMENT '拥挤等级',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    FOREIGN KEY (attraction_id) REFERENCES attractions(id) ON DELETE CASCADE,
    INDEX idx_attraction_id (attraction_id),
    INDEX idx_update_time (update_time),
    INDEX idx_crowd_level (crowd_level)
) COMMENT='景点实时状态表' ENGINE=InnoDB;

-- 插入城市数据
INSERT INTO cities (name, country, province, latitude, longitude, description, cover_image) VALUES 
('北京', '中国', '北京', 39.9042, 116.4074, '中国首都，历史文化名城', 'https://cityimg.com/beijing.jpg'),
('上海', '中国', '上海', 31.2304, 121.4737, '中国经济中心，国际化大都市', 'https://cityimg.com/shanghai.jpg'),
('广州', '中国', '广东', 23.1291, 113.2644, '华南地区经济中心，历史文化名城', 'https://cityimg.com/guangzhou.jpg'),
('深圳', '中国', '广东', 22.5431, 114.0579, '中国改革开放前沿，科技创新城市', 'https://cityimg.com/shenzhen.jpg'),
('成都', '中国', '四川', 30.5728, 104.0668, '西南地区中心城市，休闲文化之都', 'https://cityimg.com/chengdu.jpg'),
('西安', '中国', '陕西', 34.3416, 108.9398, '十三朝古都，历史文化名城', 'https://cityimg.com/xian.jpg'),
('杭州', '中国', '浙江', 30.2741, 120.1551, '江南水乡，风景秀丽', 'https://cityimg.com/hangzhou.jpg'),
('重庆', '中国', '重庆', 29.5630, 106.5516, '山城，火锅文化之都', 'https://cityimg.com/chongqing.jpg'),
('苏州', '中国', '江苏', 31.2989, 120.5853, '园林城市，江南水乡', 'https://cityimg.com/suzhou.jpg');

-- 插入用户数据
INSERT INTO users (username, email, password, avatar, phone) VALUES 
('zhangsan', 'zhangsan@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/1.jpg', '13800138001'),
('lisi', 'lisi@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/2.jpg', '13800138002'),
('wangwu', 'wangwu@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/3.jpg', '13800138003'),
('zhaoliu', 'zhaoliu@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/4.jpg', '13800138004'),
('sunqi', 'sunqi@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/5.jpg', '13800138005'),
('zhouba', 'zhouba@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/6.jpg', '13800138006'),
('wujiu', 'wujiu@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/7.jpg', '13800138007'),
('zhengshi', 'zhengshi@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/8.jpg', '13800138008'),
('chenshiyi', 'chenshiyi@example.com', 'e10adc3949ba59abbe56e057f20f883e', 'https://avatar1.com/9.jpg', '13800138009');

-- 插入景点数据
INSERT INTO attractions (name, city_id, address, description, ticket_price, opening_hours, latitude, longitude, images, rating, view_count) VALUES 
('故宫博物院', 1, '北京市东城区景山前街4号', '明清皇家宫殿，世界文化遗产', 60.00, '8:30-17:00（周一闭馆）', 39.916527, 116.390189, '["https://attracimg.com/gugong1.jpg","https://attracimg.com/gugong2.jpg"]', 4.9, 125800),
('八达岭长城', 1, '北京市延庆区军都山关沟古道北口', '万里长城的精华段', 40.00, '7:30-17:30', 40.357494, 115.992817, '["https://attracimg.com/greatwall1.jpg","https://attracimg.com/greatwall2.jpg"]', 4.8, 98600),
('上海迪士尼乐园', 2, '上海市浦东新区川沙新镇申迪北路753号', '魔法主题乐园', 435.00, '9:00-21:30', 31.144258, 121.657004, '["https://attracimg.com/disney1.jpg","https://attracimg.com/disney2.jpg"]', 4.7, 156200),
('外滩', 2, '上海市黄浦区中山东一路', '上海城市象征，万国建筑博览群', 0.00, '全天开放', 31.230454, 121.490170, '["https://attracimg.com/thebund1.jpg","https://attracimg.com/thebund2.jpg"]', 4.8, 210500),
('广州塔', 3, '广州市海珠区阅江西路222号', '中国第一高塔，小蛮腰', 150.00, '9:30-22:30', 23.114157, 113.318985, '["https://attracimg.com/guangzhouta1.jpg","https://attracimg.com/guangzhouta2.jpg"]', 4.7, 89300),
('宽窄巷子', 5, '成都市青羊区金河路口宽窄巷子', '明清民居建筑群，成都特色', 0.00, '全天开放', 30.579414, 104.064853, '["https://attracimg.com/kuanzhai1.jpg","https://attracimg.com/kuanzhai2.jpg"]', 4.6, 76500),
('兵马俑博物馆', 6, '西安市临潼区秦陵北路', '世界第八大奇迹', 120.00, '8:30-18:00', 34.387522, 109.276605, '["https://attracimg.com/bingmayong1.jpg","https://attracimg.com/bingmayong2.jpg"]', 4.9, 132700),
('西湖', 7, '杭州市西湖区西湖风景区', '中国十大风景名胜之一', 0.00, '全天开放', 30.259068, 120.145617, '["https://attracimg.com/xihu1.jpg","https://attracimg.com/xihu2.jpg"]', 4.9, 235800),
('洪崖洞', 8, '重庆市渝中区嘉陵江滨江路88号', '吊脚楼建筑群，夜景绝美', 0.00, '11:00-23:00', 29.564740, 106.501402, '["https://attracimg.com/hongyadong1.jpg","https://attracimg.com/hongyadong2.jpg"]', 4.8, 187600);

-- 插入交通工具数据
INSERT INTO transport (transport_name, transport_type, icon_url, avg_speed_kmh, cost_per_km, co2_emission, description) VALUES 
('步行', 'walking', 'https://transportimg.com/walking.png', 5.00, 0.00, 0.000, '环保无成本，适合短距离出行'),
('公交', 'bus', 'https://transportimg.com/bus.png', 25.00, 2.00, 0.120, '覆盖广，性价比高'),
('地铁', 'subway', 'https://transportimg.com/subway.png', 40.00, 3.00, 0.080, '快速准时，不受拥堵影响'),
('出租车', 'taxi', 'https://transportimg.com/taxi.png', 35.00, 2.50, 0.150, '灵活便捷，适合多人同行'),
('自驾车', 'car', 'https://transportimg.com/car.png', 50.00, 1.80, 0.180, '自由度高，适合远途出行'),
('高铁', 'train', 'https://transportimg.com/train.png', 180.00, 0.50, 0.050, '高速舒适，跨城首选'),
('自行车', 'bicycle', 'https://transportimg.com/bicycle.png', 12.00, 0.00, 0.000, '健康环保，适合短途游览'),
('游船', 'boat', 'https://transportimg.com/boat.png', 15.00, 5.00, 0.030, '观光休闲，适合水景景点'),
('飞机', 'plane', 'https://transportimg.com/plane.png', 800.00, 3.00, 0.250, '长途快速，跨区域出行');

-- 插入路线数据
INSERT INTO routes (title, description, city_id, duration_days, difficulty, cover_image, user_id, view_count, like_count, is_public) VALUES 
('北京经典3日游', '涵盖故宫、长城等核心景点，感受古都魅力', 1, 3, '简单', 'https://routeimg.com/beijing3d.jpg', 1, 5620, 1258, TRUE),
('上海亲子2日游', '迪士尼+外滩，适合家庭出行', 2, 2, '简单', 'https://routeimg.com/shanghai2d.jpg', 2, 4890, 987, TRUE),
('广州美食3日游', '早茶+景点，品味岭南文化', 3, 3, '简单', 'https://routeimg.com/guangzhou3d.jpg', 3, 3250, 765, TRUE),
('成都休闲4日游', '慢游宽窄巷子、都江堰，体验巴适生活', 5, 4, '中等', 'https://routeimg.com/chengdu4d.jpg', 4, 6120, 1342, TRUE),
('西安历史5日游', '深度探访十三朝古都，穿越千年时光', 6, 5, '中等', 'https://routeimg.com/xian5d.jpg', 5, 5980, 1123, TRUE),
('杭州诗意2日游', '西湖+灵隐寺，感受江南韵味', 7, 2, '简单', 'https://routeimg.com/hangzhou2d.jpg', 6, 7350, 1890, TRUE),
('重庆山城3日游', '洪崖洞+磁器口，体验立体城市', 8, 3, '中等', 'https://routeimg.com/chongqing3d.jpg', 7, 4560, 876, TRUE),
('苏州园林2日游', '拙政园+平江路，品味园林艺术', 9, 2, '简单', 'https://routeimg.com/suzhou2d.jpg', 8, 3890, 654, TRUE),
('深圳科技1日游', '腾讯滨海大厦+华强北，感受科技魅力', 4, 1, '简单', 'https://routeimg.com/shenzhen1d.jpg', 9, 2980, 432, TRUE);

-- 插入路线-景点关联数据
INSERT INTO route_attractions (route_id, attraction_id, day_number, visit_order, notes) VALUES 
(1, 1, 1, 1, '早上8点入园，避开人流'),
(1, 2, 2, 1, '建议乘坐缆车上下山'),
(2, 3, 1, 1, '提前下载迪士尼APP，绑定门票'),
(2, 4, 2, 1, '晚上7点看外滩灯光秀'),
(3, 5, 1, 1, '登塔最佳时间18:00-20:00'),
(4, 6, 1, 1, '推荐品尝巷子里的地道火锅'),
(5, 7, 1, 1, '请专业导游讲解，体验更佳'),
(6, 8, 1, 1, '租一艘小船游西湖，别有韵味'),
(7, 9, 1, 1, '晚上21:00后拍照效果最好');

-- 插入路线交通关联数据
INSERT INTO route_transport (route_id, from_attraction_id, to_attraction_id, transport_id, transport_order, estimated_duration, distance, instructions, cost_estimate) VALUES 
(1, 1, 2, 5, 1, 120, 75.00, '走京藏高速，避开早高峰', 135.00),
(2, 3, 4, 3, 1, 45, 15.00, '乘坐地铁11号线转2号线', 6.00),
(3, 5, 3, 2, 1, 30, 8.00, '乘坐公交2号线直达', 4.00),
(4, 6, 7, 6, 1, 90, 60.00, '乘坐成灌高铁，半小时一班', 30.00),
(5, 7, 8, 4, 1, 40, 20.00, '打出租车约50元，车程30分钟', 50.00),
(6, 8, 9, 8, 1, 60, 5.00, '西湖游船，从断桥到三潭印月', 25.00),
(7, 9, 1, 7, 1, 20, 3.00, '沿嘉陵江滨江路骑行，风景优美', 0.00),
(8, 2, 5, 2, 1, 25, 6.00, '乘坐公交游2路，直达拙政园', 3.00),
(9, 4, 6, 1, 1, 15, 1.50, '步行穿过科技园区，感受创新氛围', 0.00);

-- 插入景点实时状态数据
INSERT INTO attraction_realtime_status (attraction_id, weather, temperature, crowd_count, crowd_level) VALUES 
(1, '晴', 25, 5000, 3),
(2, '多云', 23, 3000, 2),
(3, '晴', 28, 8000, 4),
(4, '晴', 26, 12000, 5),
(5, '多云', 30, 2000, 2),
(6, '阴', 24, 1500, 1),
(7, '晴', 27, 4000, 3),
(8, '小雨', 22, 6000, 4),
(9, '晴', 25, 7000, 4);

-- 创建必要的索引和约束
-- 景点表索引
CREATE INDEX idx_attractions_city_id ON attractions(city_id);
CREATE INDEX idx_attractions_name ON attractions(name);
CREATE INDEX idx_attractions_rating ON attractions(rating);

-- 路线表索引
CREATE INDEX idx_routes_city_id ON routes(city_id);
CREATE INDEX idx_routes_user_id ON routes(user_id);
CREATE INDEX idx_routes_view_count ON routes(view_count);
CREATE INDEX idx_routes_like_count ON routes(like_count);

-- 路线景点关联表索引
CREATE INDEX idx_route_attractions_route_id ON route_attractions(route_id);
CREATE INDEX idx_route_attractions_attraction_id ON route_attractions(attraction_id);
CREATE INDEX idx_route_attractions_day_number ON route_attractions(day_number);

-- 路线交通表索引
CREATE INDEX idx_route_transport_route_id ON route_transport(route_id);
CREATE INDEX idx_route_transport_from_attraction_id ON route_transport(from_attraction_id);
CREATE INDEX idx_route_transport_to_attraction_id ON route_transport(to_attraction_id);
CREATE INDEX idx_route_transport_transport_id ON route_transport(transport_id);

-- 景点实时状态表索引
CREATE INDEX idx_attraction_realtime_status_attraction_id ON attraction_realtime_status(attraction_id);
CREATE INDEX idx_attraction_realtime_status_update_time ON attraction_realtime_status(update_time);
CREATE INDEX idx_attraction_realtime_status_crowd_level ON attraction_realtime_status(crowd_level);

-- 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);

-- 交通表索引
CREATE INDEX idx_transport_transport_type ON transport(transport_type);
CREATE INDEX idx_transport_avg_speed_kmh ON transport(avg_speed_kmh);
CREATE INDEX idx_transport_cost_per_km ON transport(cost_per_km);

-- 城市表索引
CREATE INDEX idx_cities_name ON cities(name);

-- 资源文件表
CREATE TABLE IF NOT EXISTS resource_file (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '资源文件ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    upload_user_id INT COMMENT '上传用户ID',
    description TEXT COMMENT '文件描述',
    status INT DEFAULT 1 COMMENT '状态(1:正常, 0:禁用)',
    route_id INT COMMENT '关联路线ID',
    tags VARCHAR(500) COMMENT '标签',
    preview_url VARCHAR(500) COMMENT '预览URL',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    rating DECIMAL(3, 2) DEFAULT 0 COMMENT '评分',
    last_access_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    share_url VARCHAR(500) COMMENT '分享URL',
    share_expire_time TIMESTAMP COMMENT '分享过期时间',
    version INT DEFAULT 1 COMMENT '文件版本',
    parent_file_id INT COMMENT '父文件ID',
    file_category VARCHAR(100) COMMENT '文件分类',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (upload_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_file_id) REFERENCES resource_file(id) ON DELETE SET NULL,
    INDEX idx_upload_user_id (upload_user_id),
    INDEX idx_file_name (file_name),
    INDEX idx_file_type (file_type),
    INDEX idx_status (status),
    INDEX idx_route_id (route_id),
    INDEX idx_tags (tags),
    INDEX idx_download_count (download_count),
    INDEX idx_rating (rating),
    INDEX idx_last_access_time (last_access_time),
    INDEX idx_share_url (share_url),
    INDEX idx_file_category (file_category),
    INDEX idx_parent_file_id (parent_file_id),
    INDEX idx_view_count (view_count)
) COMMENT='资源文件表' ENGINE=InnoDB;

-- 资源文件表索引
CREATE INDEX idx_resource_file_upload_time ON resource_file(upload_time);
CREATE INDEX idx_resource_file_file_size ON resource_file(file_size);

-- 文件标签表
CREATE TABLE IF NOT EXISTS file_tag (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称',
    tag_type VARCHAR(50) COMMENT '标签类型',
    file_id INT NOT NULL COMMENT '文件ID',
    user_id INT COMMENT '创建用户ID',
    description TEXT COMMENT '标签描述',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES resource_file(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_file_id (file_id),
    INDEX idx_tag_name (tag_name),
    INDEX idx_tag_type (tag_type),
    INDEX idx_user_id (user_id),
    INDEX idx_usage_count (usage_count)
) COMMENT='文件标签表' ENGINE=InnoDB;

-- 文件评论表
CREATE TABLE IF NOT EXISTS file_comment (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    file_id INT NOT NULL COMMENT '文件ID',
    user_id INT COMMENT '评论用户ID',
    user_name VARCHAR(100) COMMENT '用户名',
    content TEXT NOT NULL COMMENT '评论内容',
    rating INT COMMENT '评分(1-5)',
    parent_id INT COMMENT '父评论ID',
    likes INT DEFAULT 0 COMMENT '点赞数',
    status INT DEFAULT 1 COMMENT '状态(1:正常, 0:禁用)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES resource_file(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES file_comment(id) ON DELETE SET NULL,
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_rating (rating),
    INDEX idx_likes (likes),
    INDEX idx_status (status)
) COMMENT='文件评论表' ENGINE=InnoDB;

-- 路线分享表
CREATE TABLE IF NOT EXISTS route_shares (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
    route_id INT NOT NULL COMMENT '路线ID',
    user_id INT NOT NULL COMMENT '分享用户ID',
    share_code VARCHAR(32) NOT NULL COMMENT '分享码',
    share_title VARCHAR(100) NOT NULL COMMENT '分享标题',
    share_description TEXT COMMENT '分享描述',
    share_count INT DEFAULT 0 COMMENT '分享次数',
    visit_count INT DEFAULT 0 COMMENT '访问次数',
    expire_time TIMESTAMP COMMENT '过期时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_share_code (share_code),
    INDEX idx_route_id (route_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_active (is_active)
) COMMENT='路线分享表' ENGINE=InnoDB;

-- 路线评价表
CREATE TABLE IF NOT EXISTS route_comments (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    route_id INT NOT NULL COMMENT '路线ID',
    user_id INT NOT NULL COMMENT '用户ID',
    rating DECIMAL(3,2) DEFAULT 5.0 COMMENT '评分',
    content TEXT NOT NULL COMMENT '评论内容',
    images TEXT COMMENT '评论图片',
    likes_count INT DEFAULT 0 COMMENT '点赞数',
    is_anonymous BOOLEAN DEFAULT FALSE COMMENT '是否匿名',
    is_published BOOLEAN DEFAULT TRUE COMMENT '是否发布',
    reply_to INT COMMENT '回复评论ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_route_id (route_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating),
    INDEX idx_is_published (is_published),
    INDEX idx_reply_to (reply_to)
) COMMENT='路线评价表' ENGINE=InnoDB;

-- 路线收藏表
CREATE TABLE IF NOT EXISTS route_collections (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    route_id INT NOT NULL COMMENT '路线ID',
    user_id INT NOT NULL COMMENT '用户ID',
    collection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    notes TEXT COMMENT '收藏备注',
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_route_user (route_id, user_id),
    INDEX idx_route_id (route_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_public (is_public)
) COMMENT='路线收藏表' ENGINE=InnoDB;
