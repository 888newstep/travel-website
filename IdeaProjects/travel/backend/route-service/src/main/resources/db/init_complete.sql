-- ============================================================
-- 旅游网站数据库 - 完整初始化脚本
-- 修正：表名匹配实体、UNIQUE约束、补全ui_dictionary
-- 20 张表（合并 route_collection + travel_note_collection → user_collection）
-- ============================================================

-- 删除旧数据库（如需要）
-- DROP DATABASE IF EXISTS travel_website;

CREATE DATABASE IF NOT EXISTS travel_website
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE travel_website;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  UNIQUE NOT NULL COMMENT '用户名',
    email       VARCHAR(100) UNIQUE COMMENT '邮箱（手机号注册时可为空）',
    password    VARCHAR(255) NOT NULL COMMENT '密码',
    avatar      VARCHAR(200) COMMENT '头像URL',
    phone       VARCHAR(20)  COMMENT '手机号',
    user_type   INT NOT NULL DEFAULT 1 COMMENT '用户类型(1:普通用户,9:管理员)',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) COMMENT='用户表' ENGINE=InnoDB;

-- ============================================================
-- 2. 城市表
-- ============================================================
CREATE TABLE IF NOT EXISTS `city` (
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '城市ID',
    name         VARCHAR(50) NOT NULL COMMENT '城市名称',
    country      VARCHAR(50) COMMENT '国家',
    province     VARCHAR(50) COMMENT '省份',
    latitude     DECIMAL(10, 8) COMMENT '纬度',
    longitude    DECIMAL(11, 8) COMMENT '经度',
    description  TEXT COMMENT '城市简介',
    cover_image  VARCHAR(200) COMMENT '封面图URL',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_name (name),
    INDEX idx_country (country),
    INDEX idx_province (province)
) COMMENT='城市表' ENGINE=InnoDB;

-- ============================================================
-- 3. 景点表
-- ============================================================
CREATE TABLE IF NOT EXISTS `attraction` (
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT '景点ID',
    name           VARCHAR(100) NOT NULL COMMENT '景点名称',
    city_id        INT NOT NULL COMMENT '城市ID',
    address        VARCHAR(200) COMMENT '地址',
    description    TEXT COMMENT '景点描述',
    ticket_price   DECIMAL(10, 2) DEFAULT 0 COMMENT '门票价格',
    opening_hours  VARCHAR(100) COMMENT '开放时间',
    latitude       DECIMAL(10, 8) COMMENT '纬度',
    longitude      DECIMAL(11, 8) COMMENT '经度',
    images         TEXT COMMENT '图片(JSON数组)',
    rating         DECIMAL(3, 2) DEFAULT 0 COMMENT '综合评分',
    view_count     INT DEFAULT 0 COMMENT '浏览数',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE CASCADE,
    INDEX idx_city (city_id),
    INDEX idx_rating (rating DESC),
    INDEX idx_name (name)
) COMMENT='景点表' ENGINE=InnoDB;

-- ============================================================
-- 4. 路线表
-- ============================================================
CREATE TABLE IF NOT EXISTS `route` (
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '路线ID',
    title         VARCHAR(100) NOT NULL COMMENT '路线标题',
    description   TEXT COMMENT '路线描述',
    city_id       INT NOT NULL COMMENT '城市ID',
    duration_days INT DEFAULT 1 COMMENT '天数',
    difficulty    VARCHAR(20) DEFAULT '中等' COMMENT '难度',
    cover_image   VARCHAR(200) COMMENT '封面图URL',
    user_id       INT COMMENT '创建用户ID',
    view_count    INT DEFAULT 0 COMMENT '浏览数',
    like_count    INT DEFAULT 0 COMMENT '点赞数',
    is_public     BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_city (city_id),
    INDEX idx_user (user_id),
    INDEX idx_public (is_public),
    INDEX idx_views (view_count DESC)
) COMMENT='路线表' ENGINE=InnoDB;

-- ============================================================
-- 5. 路线-景点关联表 (注意：@TableName("route_attractions"))
-- ============================================================
CREATE TABLE IF NOT EXISTS `route_attractions` (
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    route_id       INT NOT NULL COMMENT '路线ID',
    attraction_id  INT NOT NULL COMMENT '景点ID',
    day_number     INT NOT NULL DEFAULT 1 COMMENT '第几天',
    visit_order    INT NOT NULL COMMENT '游览顺序',
    notes          TEXT COMMENT '备注',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    FOREIGN KEY (attraction_id) REFERENCES attraction(id) ON DELETE CASCADE,
    UNIQUE KEY uk_route_attraction (route_id, attraction_id),
    UNIQUE KEY uk_route_day_visit_order (route_id, day_number, visit_order),
    INDEX idx_route (route_id),
    INDEX idx_attraction (attraction_id),
    INDEX idx_day (route_id, day_number)
) COMMENT='路线-景点关联表' ENGINE=InnoDB;

-- ============================================================
-- 6. 交通工具表
-- ============================================================
CREATE TABLE IF NOT EXISTS `transport` (
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT '交通工具ID',
    name           VARCHAR(100) NOT NULL COMMENT '交通工具名称',
    type           ENUM('walking','bus','subway','taxi','car','train','bicycle','boat','plane') NOT NULL COMMENT '类型',
    icon_url       VARCHAR(500) COMMENT '图标URL',
    avg_speed_kmh  DECIMAL(5, 2) COMMENT '平均速度(km/h)',
    cost_per_km    DECIMAL(6, 2) COMMENT '每公里费用',
    co2_emission   DECIMAL(6, 3) COMMENT '每公里碳排放(kg)',
    description    TEXT COMMENT '描述',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type (type),
    INDEX idx_name (name)
) COMMENT='交通工具表' ENGINE=InnoDB;

-- ============================================================
-- 7. 路线交通关联表 (无对应实体，由Mapper直接操作)
-- ============================================================
CREATE TABLE IF NOT EXISTS `route_transport` (
    id                  INT PRIMARY KEY AUTO_INCREMENT COMMENT '路线交通ID',
    route_id            INT NOT NULL COMMENT '路线ID',
    from_attraction_id  INT NOT NULL COMMENT '出发景点ID',
    to_attraction_id    INT NOT NULL COMMENT '到达景点ID',
    transport_id        INT NOT NULL COMMENT '交通工具ID',
    transport_order     INT NOT NULL COMMENT '交通顺序',
    estimated_duration  INT COMMENT '预计用时(分钟)',
    distance            DECIMAL(6, 2) COMMENT '距离(公里)',
    instructions        TEXT COMMENT '交通指示',
    cost_estimate       DECIMAL(8, 2) COMMENT '费用预估',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    FOREIGN KEY (from_attraction_id) REFERENCES attraction(id) ON DELETE CASCADE,
    FOREIGN KEY (to_attraction_id) REFERENCES attraction(id) ON DELETE CASCADE,
    FOREIGN KEY (transport_id) REFERENCES transport(id) ON DELETE CASCADE,
    UNIQUE KEY uk_route_transport_order (route_id, transport_order),
    INDEX idx_route (route_id),
    INDEX idx_transport (transport_id)
) COMMENT='路线交通关联表' ENGINE=InnoDB;

-- ============================================================
-- 8. 餐厅表
-- ============================================================
CREATE TABLE IF NOT EXISTS `restaurant` (
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT '餐厅ID',
    name           VARCHAR(100) NOT NULL COMMENT '餐厅名称',
    city_id        INT NOT NULL COMMENT '城市ID',
    address        VARCHAR(200) COMMENT '地址',
    latitude       DECIMAL(10, 8) COMMENT '纬度',
    longitude      DECIMAL(11, 8) COMMENT '经度',
    rating         DECIMAL(3, 2) DEFAULT 0 COMMENT '评分',
    price_level    VARCHAR(20) COMMENT '价格等级',
    average_cost   DECIMAL(10, 2) COMMENT '人均消费',
    cuisine_type   VARCHAR(50) COMMENT '菜系类型',
    feature        VARCHAR(200) COMMENT '特色',
    phone          VARCHAR(20) COMMENT '联系电话',
    opening_hours  VARCHAR(100) COMMENT '营业时间',
    image_url      VARCHAR(200) COMMENT '图片URL',
    description    TEXT COMMENT '描述',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE CASCADE,
    INDEX idx_city (city_id),
    INDEX idx_rating (rating DESC),
    INDEX idx_cuisine (cuisine_type),
    INDEX idx_price (price_level)
) COMMENT='餐厅表' ENGINE=InnoDB;

-- ============================================================
-- 9. 游记表
-- ============================================================
CREATE TABLE IF NOT EXISTS `travel_note` (
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '游记ID',
    user_id         INT NOT NULL COMMENT '用户ID',
    title           VARCHAR(200) NOT NULL COMMENT '标题',
    content         TEXT COMMENT '内容',
    cover_image     VARCHAR(200) COMMENT '封面图URL',
    images          TEXT COMMENT '图片(JSON数组)',
    city_id         INT COMMENT '城市ID',
    views_count     INT DEFAULT 0 COMMENT '浏览数',
    likes_count     INT DEFAULT 0 COMMENT '点赞数',
    comments_count  INT DEFAULT 0 COMMENT '评论数',
    is_public       BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_city (city_id),
    INDEX idx_views (views_count DESC),
    INDEX idx_likes (likes_count DESC),
    INDEX idx_public (is_public)
) COMMENT='游记表' ENGINE=InnoDB;

-- ============================================================
-- 10. 游记标签表 (注意：@TableName("travel_note_tags"))
-- ============================================================
CREATE TABLE IF NOT EXISTS `travel_note_tags` (
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    note_id    INT NOT NULL COMMENT '游记ID',
    tag_name   VARCHAR(50) NOT NULL COMMENT '标签名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (note_id) REFERENCES travel_note(id) ON DELETE CASCADE,
    UNIQUE KEY uk_note_tag (note_id, tag_name),
    INDEX idx_note (note_id),
    INDEX idx_tag (tag_name)
) COMMENT='游记标签表' ENGINE=InnoDB;

-- ============================================================
-- 11. 用户收藏表 (合并 route_collection + travel_note_collection)
--     item_type: 'route' / 'travel_note' / 'route_comment'
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_collection` (
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    user_id         INT NOT NULL COMMENT '用户ID',
    item_id         INT NOT NULL COMMENT '被收藏项ID',
    item_type       VARCHAR(20) NOT NULL COMMENT '类型: route/travel_note/route_comment',
    collection_type VARCHAR(20) DEFAULT 'collect' COMMENT '收藏方式: collect/like',
    notes           TEXT COMMENT '收藏备注',
    category        VARCHAR(50) COMMENT '分类',
    is_public       BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_item_action (user_id, item_id, item_type, collection_type),
    INDEX idx_user (user_id),
    INDEX idx_item (item_id, item_type),
    INDEX idx_category (category)
) COMMENT='用户行为表(收藏+点赞)' ENGINE=InnoDB;

-- ============================================================
-- 12. 路线评论表 (合并 route_rating + route_feedback)
-- ============================================================
CREATE TABLE IF NOT EXISTS `route_comment` (
    id                     INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    route_id               INT NOT NULL COMMENT '路线ID',
    user_id                INT NOT NULL COMMENT '用户ID',
    rating                 DECIMAL(3,2) DEFAULT NULL COMMENT '综合评分(1-5)',
    content                TEXT COMMENT '评论内容',
    images                 TEXT COMMENT '图片(JSON数组)',
    likes_count            INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    is_anonymous           BOOLEAN DEFAULT FALSE COMMENT '是否匿名',
    is_published           BOOLEAN DEFAULT TRUE COMMENT '是否发布',
    reply_to               INT COMMENT '回复评论ID',
    comfort_rating         INT COMMENT '舒适度评分(1-5)',
    transport_rating       INT COMMENT '交通便利性评分(1-5)',
    dining_rating          INT COMMENT '餐饮体验评分(1-5)',
    feedback_type          VARCHAR(30) COMMENT '反馈类型',
    tags                   VARCHAR(500) COMMENT '标签(JSON数组)',
    improvement_suggestions TEXT COMMENT '改进建议(JSON)',
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to) REFERENCES route_comment(id) ON DELETE SET NULL,
    INDEX idx_route (route_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating DESC),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_reply_to (reply_to),
    INDEX idx_route_published_reply_created (route_id, is_published, reply_to, created_at),
    INDEX idx_user_published_created (user_id, is_published, created_at)
) COMMENT='路线评论表(合并评分+反馈)' ENGINE=InnoDB;

-- ============================================================
-- 12.5 景点点评表
-- ============================================================
CREATE TABLE IF NOT EXISTS `attraction_review` (
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '点评ID',
    attraction_id   INT NOT NULL COMMENT '景点ID',
    user_id         INT NOT NULL COMMENT '用户ID',
    rating          TINYINT NOT NULL DEFAULT 5 COMMENT '评分(1-5)',
    content         TEXT COMMENT '点评内容',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_attraction (attraction_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating DESC),
    UNIQUE KEY uk_attraction_user (attraction_id, user_id)
) COMMENT='景点点评表' ENGINE=InnoDB;

-- ============================================================
-- 13. 路线分享表
-- ============================================================
CREATE TABLE IF NOT EXISTS `route_share` (
    id                 INT PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
    route_id           INT NOT NULL COMMENT '路线ID',
    user_id            INT NOT NULL COMMENT '分享用户ID',
    share_code         VARCHAR(32) NOT NULL COMMENT '分享码',
    share_title        VARCHAR(100) COMMENT '分享标题',
    share_description  TEXT COMMENT '分享描述',
    share_count        INT DEFAULT 0 COMMENT '分享次数',
    visit_count        INT DEFAULT 0 COMMENT '访问次数',
    expire_time        TIMESTAMP NULL COMMENT '过期时间',
    is_active          BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    password           VARCHAR(50) COMMENT '访问密码',
    file_name          VARCHAR(255) COMMENT '文件名',
    item_id            INT COMMENT '通用项目ID',
    item_type          VARCHAR(30) COMMENT '通用项目类型',
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_share_code (share_code),
    INDEX idx_route (route_id),
    INDEX idx_user (user_id),
    INDEX idx_active (is_active)
) COMMENT='路线分享表' ENGINE=InnoDB;

-- ============================================================
-- 14. 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification` (
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id      INT NOT NULL COMMENT '用户ID',
    type         VARCHAR(30) NOT NULL COMMENT '通知类型',
    title        VARCHAR(100) NOT NULL COMMENT '标题',
    content      TEXT COMMENT '内容',
    is_read      BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    redirect_url VARCHAR(500) COMMENT '跳转URL',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_type (type)
) COMMENT='通知表' ENGINE=InnoDB;

-- ============================================================
-- 15. 用户反馈表
-- ============================================================
CREATE TABLE IF NOT EXISTS `feedback` (
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    user_id       INT COMMENT '用户ID',
    type          VARCHAR(30) NOT NULL COMMENT '反馈类型',
    content       TEXT NOT NULL COMMENT '反馈内容',
    contact_info  VARCHAR(100) COMMENT '联系方式',
    status        VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    reply_content TEXT COMMENT '回复内容',
    reply_time    TIMESTAMP NULL COMMENT '回复时间',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type)
) COMMENT='用户反馈表' ENGINE=InnoDB;

-- ============================================================
-- 16. 资源文件表 (合并 file_share + file_version + file_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `resource_file` (
    id                INT PRIMARY KEY AUTO_INCREMENT COMMENT '资源文件ID',
    file_id           VARCHAR(36) COMMENT '文件UUID',
    file_name         VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path         VARCHAR(500) COMMENT '文件存储路径',
    file_size         BIGINT COMMENT '文件大小(字节)',
    file_type         VARCHAR(50) COMMENT '文件类型/扩展名',
    upload_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    upload_user_id    INT COMMENT '上传用户ID',
    description       TEXT COMMENT '文件描述',
    status            INT DEFAULT 1 COMMENT '状态(1:正常,0:禁用)',
    route_id          INT COMMENT '关联路线ID',
    tags              VARCHAR(500) COMMENT '标签',
    preview_url       VARCHAR(500) COMMENT '预览URL',
    download_count    INT DEFAULT 0 COMMENT '下载次数',
    comment_count     INT DEFAULT 0 COMMENT '评论次数',
    rating            DECIMAL(3, 2) DEFAULT 0 COMMENT '评分',
    view_count        INT DEFAULT 0 COMMENT '浏览次数',
    file_category     VARCHAR(100) COMMENT '文件分类',
    version           INT DEFAULT 1 COMMENT '当前版本号',
    parent_file_id    INT COMMENT '父文件ID(版本追溯)',
    share_url         VARCHAR(500) COMMENT '分享URL',
    share_expire_time TIMESTAMP NULL COMMENT '分享过期时间',
    last_access_time  TIMESTAMP NULL COMMENT '最后访问时间',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (upload_user_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (route_id) REFERENCES route(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_file_id) REFERENCES resource_file(id) ON DELETE SET NULL,
    INDEX idx_upload_user (upload_user_id),
    INDEX idx_file_name (file_name),
    INDEX idx_file_type (file_type),
    INDEX idx_status (status),
    INDEX idx_route (route_id),
    INDEX idx_category (file_category),
    INDEX idx_parent (parent_file_id),
    INDEX idx_download_count (download_count DESC),
    INDEX idx_rating (rating DESC),
    INDEX idx_view_count (view_count DESC),
    INDEX idx_share_url (share_url)
) COMMENT='资源文件表(合并分类+版本+分享)' ENGINE=InnoDB;

-- ============================================================
-- 17. 文件标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS `file_tag` (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name    VARCHAR(100) NOT NULL COMMENT '标签名称',
    tag_type    VARCHAR(50) COMMENT '标签类型',
    file_id     INT NOT NULL COMMENT '文件ID',
    user_id     INT COMMENT '创建用户ID',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (file_id) REFERENCES resource_file(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_file (file_id),
    INDEX idx_tag_name (tag_name),
    INDEX idx_tag_type (tag_type),
    INDEX idx_usage (usage_count DESC)
) COMMENT='文件标签表' ENGINE=InnoDB;

-- ============================================================
-- 18. 文件评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `file_comment` (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    file_id     INT NOT NULL COMMENT '文件ID',
    user_id     INT COMMENT '评论用户ID',
    user_name   VARCHAR(100) COMMENT '用户名',
    content     TEXT NOT NULL COMMENT '评论内容',
    rating      INT COMMENT '评分(1-5)',
    parent_id   INT COMMENT '父评论ID(回复)',
    likes       INT DEFAULT 0 COMMENT '点赞数',
    status      INT DEFAULT 1 COMMENT '状态(1:正常,0:禁用)',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (file_id) REFERENCES resource_file(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES file_comment(id) ON DELETE SET NULL,
    INDEX idx_file (file_id),
    INDEX idx_user (user_id),
    INDEX idx_parent (parent_id),
    INDEX idx_rating (rating),
    INDEX idx_likes (likes),
    INDEX idx_status (status)
) COMMENT='文件评论表' ENGINE=InnoDB;

-- ============================================================
-- 19. 景点实时状态表 (修复: 增加UNIQUE约束防止重复)
-- ============================================================
CREATE TABLE IF NOT EXISTS `attraction_realtime_status` (
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    attraction_id  INT NOT NULL COMMENT '景点ID',
    weather        VARCHAR(50) COMMENT '天气',
    temperature    INT COMMENT '温度(℃)',
    crowd_count    INT COMMENT '实时人数',
    crowd_level    INT COMMENT '拥挤等级(1-5)',
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted        TINYINT DEFAULT 0 COMMENT '删除标记',
    FOREIGN KEY (attraction_id) REFERENCES attraction(id) ON DELETE CASCADE,
    UNIQUE KEY uk_attraction (attraction_id),
    INDEX idx_crowd_level (crowd_level),
    INDEX idx_update_time (update_time)
) COMMENT='景点实时状态表' ENGINE=InnoDB;

-- ============================================================
-- 20. UI字典表 (补全：之前SQL中遗漏，但实体/mapper均存在)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ui_dictionary` (
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
    dict_type  VARCHAR(50)  NOT NULL COMMENT '字典类型',
    dict_key   VARCHAR(100) NOT NULL COMMENT '字典键',
    dict_value VARCHAR(500) COMMENT '字典值',
    dict_label VARCHAR(100) COMMENT '字典标签',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_key (dict_type, dict_key),
    INDEX idx_dict_type (dict_type)
) COMMENT='UI字典表' ENGINE=InnoDB;


-- ============================================================
-- 插入初始数据
-- ============================================================

-- -------------------------------------------------------
-- 城市 (15条)
-- -------------------------------------------------------
INSERT INTO city (name, country, province, latitude, longitude, description, cover_image) VALUES
('北京', '中国', '北京', 39.9042, 116.4074, '中国首都，历史文化名城',                 'https://cityimg.com/beijing.jpg'),
('上海', '中国', '上海', 31.2304, 121.4737, '中国经济中心，国际化大都市',             'https://cityimg.com/shanghai.jpg'),
('广州', '中国', '广东', 23.1291, 113.2644, '华南地区经济中心，美食之都',             'https://cityimg.com/guangzhou.jpg'),
('深圳', '中国', '广东', 22.5431, 114.0579, '中国改革开放前沿，科技创新城市',         'https://cityimg.com/shenzhen.jpg'),
('成都', '中国', '四川', 30.5728, 104.0668, '西南地区中心城市，休闲文化之都',         'https://cityimg.com/chengdu.jpg'),
('西安', '中国', '陕西', 34.3416, 108.9398, '十三朝古都，历史文化名城',               'https://cityimg.com/xian.jpg'),
('杭州', '中国', '浙江', 30.2741, 120.1551, '江南水乡，风景秀丽',                     'https://cityimg.com/hangzhou.jpg'),
('重庆', '中国', '重庆', 29.5630, 106.5516, '山城，火锅文化之都',                     'https://cityimg.com/chongqing.jpg'),
('苏州', '中国', '江苏', 31.2989, 120.5853, '园林城市，江南水乡',                     'https://cityimg.com/suzhou.jpg'),
('南京', '中国', '江苏', 32.0603, 118.7969, '六朝古都，人文荟萃',                     'https://cityimg.com/nanjing.jpg'),
('武汉', '中国', '湖北', 30.5928, 114.3055, '九省通衢，樱花之城',                     'https://cityimg.com/wuhan.jpg'),
('长沙', '中国', '湖南', 28.2282, 112.9388, '娱乐之都，美食天堂',                     'https://cityimg.com/changsha.jpg'),
('厦门', '中国', '福建', 24.4798, 118.0894, '海上花园，文艺清新',                     'https://cityimg.com/xiamen.jpg'),
('青岛', '中国', '山东', 36.0671, 120.3826, '帆船之都，啤酒之城',                     'https://cityimg.com/qingdao.jpg'),
('昆明', '中国', '云南', 25.0389, 102.7183, '春城花都，四季如春',                     'https://cityimg.com/kunming.jpg');

-- -------------------------------------------------------
-- 用户 (15条, 密码列为 BCrypt 哈希示例值)
-- -------------------------------------------------------
INSERT INTO `user` (username, email, password, avatar, phone) VALUES
('zhangsan',    'zhangsan@example.com',    '$2a$10$JL77.aW4Cm17w2DTn8hozut4F/U3zouGqD4XUUNy2XgR6Cu6jG7Bm', 'https://avatar1.com/1.jpg',  '13800138001'),
('lisi',        'lisi@example.com',        '$2a$10$yOBX0TAHFrczMPIMu4RL2.c62qYmdI3YgVJMfNnDvZ54RXY4Ooh5q', 'https://avatar1.com/2.jpg',  '13800138002'),
('wangwu',      'wangwu@example.com',      '$2a$10$67/c/Hg0.YC7xEJX2KfRbem6Fiktn0OHH1rtnvLDb66AZsjnbI8Jm', 'https://avatar1.com/3.jpg',  '13800138003'),
('zhaoliu',     'zhaoliu@example.com',     '$2a$10$3K/F9lWZxs8csoOQId00Ru5nkGNCarZzqPVeEDNxHKsrnDzrGXvfi', 'https://avatar1.com/4.jpg',  '13800138004'),
('sunqi',       'sunqi@example.com',       '$2a$10$CwYKWocvl7ZOrMP.4bAYpOLg9ZPmeRC7uoxOSPZ02vmyLtmgN1rtW', 'https://avatar1.com/5.jpg',  '13800138005'),
('zhouba',      'zhouba@example.com',      '$2a$10$FdTTD8lsdbFUfwrAD1m/q.D.f.8p0BK8GigagBkRZAn0qNV.cRhXa', 'https://avatar1.com/6.jpg',  '13800138006'),
('wujiu',       'wujiu@example.com',       '$2a$10$I.nutD/sVdFCzwChgkofY.vRgiC.VhmychiHx.K06eRxaV.P/Z21.', 'https://avatar1.com/7.jpg',  '13800138007'),
('zhengshi',    'zhengshi@example.com',    '$2a$10$E5z0QgnDEcQzq15QCzfUleUAfAREt234pS0k4cmE3zJId4TpzJfGC', 'https://avatar1.com/8.jpg',  '13800138008'),
('chenshiyi',   'chenshiyi@example.com',   '$2a$10$2y7fXBk5V0ZCWu3AK1OvSuuEdAn8HjnJexhJmS8prhpA4sUSpkZCi', 'https://avatar1.com/9.jpg',  '13800138009'),
('liuqian',     'liuqian@example.com',     '$2a$10$ekn/U8OP.H2H3FWP1X8cCe.XZDPzlJVVVmJ670JmMCl5Uz4JUU0Cy', 'https://avatar1.com/10.jpg', '13800138010'),
('huangyi',     'huangyi@example.com',     '$2a$10$Lu.GnomXrNl3uLRReufCaeZjZCbha2zh3pzziLcN2TqtSD23s408O', 'https://avatar1.com/11.jpg', '13800138011'),
('xulei',       'xulei@example.com',       '$2a$10$4lP0at/I8ro8PqFn5iySyu52ubxlC2XxY/kwW.RY1NSqcbn/Ljxmu', 'https://avatar1.com/12.jpg', '13800138012'),
('mayun',       'mayun@example.com',       '$2a$10$6aCKgOlighaUdHeCUEt4hOQ5fy.KkQ.fQJuSZaEEsnLx.uu/V0wZu', 'https://avatar1.com/13.jpg', '13800138013'),
('linfen',      'linfen@example.com',      '$2a$10$BdBh0WqZ/IM1pTXR6R5URO2OL.pugQpmNv6GRwaGuJWNokGGeUrJO', 'https://avatar1.com/14.jpg', '13800138014'),
('guoxia',      'guoxia@example.com',      '$2a$10$1jHY2Gus82LoopnsgdcLNO98VG1CbXx2bmiVQAY8c9brRrIldsIIK', 'https://avatar1.com/15.jpg', '13800138015');

-- -------------------------------------------------------
-- 景点 (20条)
-- -------------------------------------------------------
INSERT INTO attraction (name, city_id, address, description, ticket_price, opening_hours, latitude, longitude, images, rating, view_count) VALUES
('故宫博物院',      1,  '北京市东城区景山前街4号',            '明清皇家宫殿，世界文化遗产',           60.00,  '8:30-17:00（周一闭馆）',  39.916527, 116.390189, '["https://attracimg.com/gugong1.jpg","https://attracimg.com/gugong2.jpg"]',                    4.9, 125800),
('八达岭长城',      1,  '北京市延庆区军都山关沟古道北口',      '万里长城的精华段',                     40.00,  '7:30-17:30',              40.357494, 115.992817, '["https://attracimg.com/greatwall1.jpg","https://attracimg.com/greatwall2.jpg"]',              4.8,  98600),
('天坛公园',        1,  '北京市东城区天坛内东里7号',            '明清两代皇帝祭天的场所',               15.00,  '6:00-21:00',              39.883355, 116.407281, '["https://attracimg.com/tiantan1.jpg","https://attracimg.com/tiantan2.jpg"]',                  4.7,  65400),
('上海迪士尼乐园',  2,  '上海市浦东新区川沙新镇申迪北路753号', '魔法主题乐园',                         435.00, '9:00-21:30',              31.144258, 121.657004, '["https://attracimg.com/disney1.jpg","https://attracimg.com/disney2.jpg"]',                    4.7, 156200),
('外滩',            2,  '上海市黄浦区中山东一路',               '上海城市象征，万国建筑博览群',          0.00,   '全天开放',                31.230454, 121.490170, '["https://attracimg.com/thebund1.jpg","https://attracimg.com/thebund2.jpg"]',                  4.8, 210500),
('广州塔',          3,  '广州市海珠区阅江西路222号',            '中国第一高塔，小蛮腰',                 150.00, '9:30-22:30',              23.114157, 113.318985, '["https://attracimg.com/guangzhouta1.jpg","https://attracimg.com/guangzhouta2.jpg"]',          4.7,  89300),
('宽窄巷子',        5,  '成都市青羊区金河路口宽窄巷子',         '明清民居建筑群，成都特色',              0.00,   '全天开放',                30.579414, 104.064853, '["https://attracimg.com/kuanzhai1.jpg","https://attracimg.com/kuanzhai2.jpg"]',                4.6,  76500),
('都江堰',          5,  '成都市都江堰市公园路',                 '世界文化遗产，千年水利工程',           80.00,  '8:00-18:00',              30.571230, 103.361888, '["https://attracimg.com/dujiangyan1.jpg","https://attracimg.com/dujiangyan2.jpg"]',            4.8,  54300),
('兵马俑博物馆',    6,  '西安市临潼区秦陵北路',                 '世界第八大奇迹',                       120.00, '8:30-18:00',              34.387522, 109.276605, '["https://attracimg.com/bingmayong1.jpg","https://attracimg.com/bingmayong2.jpg"]',            4.9, 132700),
('大雁塔',          6,  '西安市雁塔区雁塔南路',                 '唐代佛教建筑艺术的杰作',               50.00,  '8:00-17:30',              34.219790, 108.959542, '["https://attracimg.com/dayanta1.jpg","https://attracimg.com/dayanta2.jpg"]',                  4.7,  98700),
('西湖',            7,  '杭州市西湖区西湖风景区',                '中国十大风景名胜之一',                 0.00,   '全天开放',                30.259068, 120.145617, '["https://attracimg.com/xihu1.jpg","https://attracimg.com/xihu2.jpg"]',                        4.9, 235800),
('灵隐寺',          7,  '杭州市西湖区法云弄1号',                '江南著名古刹，佛教圣地',               45.00,  '7:00-18:15',              30.243420, 120.099284, '["https://attracimg.com/lingyinsi1.jpg","https://attracimg.com/lingyinsi2.jpg"]',              4.8,  87600),
('洪崖洞',          8,  '重庆市渝中区嘉陵江滨江路88号',          '吊脚楼建筑群，夜景绝美',                0.00,   '11:00-23:00',             29.564740, 106.501402, '["https://attracimg.com/hongyadong1.jpg","https://attracimg.com/hongyadong2.jpg"]',            4.8, 187600),
('拙政园',          9,  '苏州市姑苏区东北街178号',              '中国四大名园之一',                     80.00,  '7:30-17:30',              31.323270, 120.629720, '["https://attracimg.com/zhuozhengyuan1.jpg","https://attracimg.com/zhuozhengyuan2.jpg"]',      4.8,  76500),
('中山陵',          10, '南京市玄武区中山门外石象路7号',        '孙中山先生陵寝，气势恢宏',              0.00,   '8:30-17:00（周一闭馆）',  32.064470, 118.848140, '["https://attracimg.com/zhongshanling1.jpg","https://attracimg.com/zhongshanling2.jpg"]',      4.8, 112400),
('黄鹤楼',          11, '武汉市武昌区蛇山西山坡特1号',          '天下江山第一楼',                       70.00,  '8:00-18:00',              30.543876, 114.296993, '["https://attracimg.com/huanghelou1.jpg","https://attracimg.com/huanghelou2.jpg"]',            4.7,  98700),
('岳麓山',          12, '长沙市岳麓区登高路58号',               '南岳七十二峰之一，千年学府',            0.00,   '6:00-23:00',              28.207680, 112.934390, '["https://attracimg.com/yuelushan1.jpg","https://attracimg.com/yuelushan2.jpg"]',              4.6,  65400),
('鼓浪屿',          13, '厦门市思明区鼓浪屿',                   '海上花园，钢琴之岛',                   35.00,  '全天开放',                24.447780, 118.066230, '["https://attracimg.com/gulangyu1.jpg","https://attracimg.com/gulangyu2.jpg"]',                4.8, 143200),
('崂山',            14, '青岛市崂山区梅岭路29号',               '海上第一名山，道教名山',               90.00,  '7:00-17:30',              36.188123, 120.629318, '["https://attracimg.com/laoshan1.jpg","https://attracimg.com/laoshan2.jpg"]',                  4.6,  76500),
('石林',            15, '昆明市石林彝族自治县',                 '世界自然遗产，喀斯特地貌奇观',         175.00, '7:30-18:00',              24.823490, 103.323724, '["https://attracimg.com/shilin1.jpg","https://attracimg.com/shilin2.jpg"]',                    4.5,  54300);

-- -------------------------------------------------------
-- 交通工具 (9条)
-- -------------------------------------------------------
INSERT INTO transport (name, type, icon_url, avg_speed_kmh, cost_per_km, co2_emission, description) VALUES
('步行',   'walking', 'https://transportimg.com/walking.png',  5.00,  0.00, 0.000, '环保无成本，适合短距离出行'),
('公交',   'bus',     'https://transportimg.com/bus.png',      25.00, 2.00, 0.120, '覆盖广，性价比高'),
('地铁',   'subway',  'https://transportimg.com/subway.png',   40.00, 3.00, 0.080, '快速准时，不受拥堵影响'),
('出租车', 'taxi',    'https://transportimg.com/taxi.png',     35.00, 2.50, 0.150, '灵活便捷，适合多人同行'),
('自驾车', 'car',     'https://transportimg.com/car.png',      50.00, 1.80, 0.180, '自由度高，适合远途出行'),
('高铁',   'train',   'https://transportimg.com/train.png',    180.00,0.50, 0.050, '高速舒适，跨城首选'),
('自行车', 'bicycle', 'https://transportimg.com/bicycle.png',  12.00, 0.00, 0.000, '健康环保，适合短途游览'),
('游船',   'boat',    'https://transportimg.com/boat.png',     15.00, 5.00, 0.030, '观光休闲，适合水景景点'),
('飞机',   'plane',   'https://transportimg.com/plane.png',    800.00,3.00, 0.250, '长途快速，跨区域出行');

-- -------------------------------------------------------
-- 路线 (15条)
-- -------------------------------------------------------
INSERT INTO route (title, description, city_id, duration_days, difficulty, cover_image, user_id, view_count, like_count, is_public) VALUES
('北京经典3日游',     '涵盖故宫、长城等核心景点，感受古都魅力',         1,  3, '简单', 'https://routeimg.com/beijing3d.jpg',     1,  5620, 1258, TRUE),
('北京深度5日游',     '故宫+长城+天坛+颐和园，全方位体验京城文化',     1,  5, '中等', 'https://routeimg.com/beijing5d.jpg',     10, 3450,  678, TRUE),
('上海亲子2日游',     '迪士尼+外滩，适合家庭出行',                      2,  2, '简单', 'https://routeimg.com/shanghai2d.jpg',    2,  4890,  987, TRUE),
('广州美食3日游',     '早茶+景点，品味岭南文化',                        3,  3, '简单', 'https://routeimg.com/guangzhou3d.jpg',   3,  3250,  765, TRUE),
('成都休闲4日游',     '慢游宽窄巷子、都江堰，体验巴适生活',             5,  4, '中等', 'https://routeimg.com/chengdu4d.jpg',     4,  6120, 1342, TRUE),
('西安历史5日游',     '深度探访十三朝古都，穿越千年时光',               6,  5, '中等', 'https://routeimg.com/xian5d.jpg',        5,  5980, 1123, TRUE),
('西安经典3日游',     '兵马俑+大雁塔+回民街，经典不容错过',            6,  3, '简单', 'https://routeimg.com/xian3d.jpg',        11, 2780,  534, TRUE),
('杭州诗意2日游',     '西湖+灵隐寺，感受江南韵味',                     7,  2, '简单', 'https://routeimg.com/hangzhou2d.jpg',    6,  7350, 1890, TRUE),
('重庆山城3日游',     '洪崖洞+磁器口，体验立体城市',                   8,  3, '中等', 'https://routeimg.com/chongqing3d.jpg',   7,  4560,  876, TRUE),
('苏州园林2日游',     '拙政园+平江路，品味园林艺术',                   9,  2, '简单', 'https://routeimg.com/suzhou2d.jpg',      8,  3890,  654, TRUE),
('深圳科技1日游',     '腾讯滨海大厦+华强北，感受科技魅力',             4,  1, '简单', 'https://routeimg.com/shenzhen1d.jpg',    9,  2980,  432, TRUE),
('南京民国2日游',     '中山陵+总统府+夫子庙，感受民国风情',             10, 2, '简单', 'https://routeimg.com/nanjing2d.jpg',     12, 1980,  345, TRUE),
('武汉江湖3日游',     '黄鹤楼+东湖+户部巷，品江湖之城',                 11, 3, '中等', 'https://routeimg.com/wuhan3d.jpg',       13, 1650,  278, TRUE),
('厦门鼓浪屿2日游',   '鼓浪屿+环岛路+曾厝垵，文艺清新之旅',            13, 2, '简单', 'https://routeimg.com/xiamen2d.jpg',      14, 3240,  567, TRUE),
('青岛海滨3日游',     '崂山+栈桥+啤酒博物馆，山海之间',                 14, 3, '中等', 'https://routeimg.com/qingdao3d.jpg',     15, 2120,  398, TRUE);

-- -------------------------------------------------------
-- 路线-景点关联 (23条)
-- -------------------------------------------------------
INSERT INTO route_attractions (route_id, attraction_id, day_number, visit_order, notes) VALUES
(1,  1,  1, 1, '早上8点入园，避开人流'),
(1,  2,  2, 1, '建议乘坐缆车上下山'),
(2,  1,  1, 1, '故宫深度游，预留4小时'),
(2,  3,  2, 1, '天坛公园晨练体验'),
(2,  2,  3, 1, '长城一日游，带好干粮'),
(3,  4,  1, 1, '提前下载迪士尼APP，绑定门票'),
(3,  5,  2, 1, '晚上7点看外滩灯光秀'),
(4,  6,  1, 1, '登塔最佳时间18:00-20:00'),
(5,  7,  1, 1, '推荐品尝巷子里的地道火锅'),
(5,  8,  2, 1, '都江堰建议请导游讲解水利原理'),
(6,  9,  1, 1, '请专业导游讲解，体验更佳'),
(6,  10, 2, 1, '大雁塔音乐喷泉晚上看最美'),
(7,  9,  1, 1, '兵马俑+华清宫一日游'),
(7,  10, 2, 1, '大雁塔+大唐不夜城'),
(8,  11, 1, 1, '租一艘小船游西湖，别有韵味'),
(8,  12, 2, 1, '灵隐寺祈福，品尝素斋'),
(9,  13, 1, 1, '晚上21:00后拍照效果最好'),
(10, 14, 1, 1, '拙政园建议早上去，人少清静'),
(12, 15, 1, 1, '中山陵392级台阶，慢慢爬'),
(13, 16, 1, 1, '黄鹤楼上远眺长江大桥'),
(14, 18, 1, 1, '鼓浪屿船票需提前网上预订'),
(15, 19, 1, 1, '崂山建议乘坐索道上山'),
(1,  3,  3, 1, '天坛回音壁体验');

-- -------------------------------------------------------
-- 路线交通数据不写入固定种子，运行时通过高德 API 获取
-- -------------------------------------------------------

-- -------------------------------------------------------
-- 景点实时状态 (20条，每个attraction_id仅一条)
-- -------------------------------------------------------
INSERT INTO attraction_realtime_status (attraction_id, weather, temperature, crowd_count, crowd_level) VALUES
(1,  '晴',    25, 5000,  3),
(2,  '多云',  23, 3000,  2),
(3,  '阴',    21, 1800,  1),
(4,  '晴',    28, 8000,  4),
(5,  '晴',    26, 12000, 5),
(6,  '多云',  30, 2000,  2),
(7,  '阴',    24, 1500,  1),
(8,  '小雨',  22, 1200,  1),
(9,  '晴',    27, 4000,  3),
(10, '晴',    28, 3200,  3),
(11, '小雨',  22, 6000,  4),
(12, '阴',    20, 2500,  2),
(13, '晴',    25, 7000,  4),
(14, '多云',  24, 3100,  3),
(15, '晴',    26, 4500,  3),
(16, '多云',  27, 2800,  2),
(17, '阴',    23, 1600,  1),
(18, '晴',    28, 8500,  5),
(19, '多云',  25, 3500,  3),
(20, '晴',    22, 2000,  2);

-- -------------------------------------------------------
-- 游记 (10条)
-- -------------------------------------------------------
INSERT INTO travel_note (user_id, title, content, cover_image, city_id, views_count, likes_count, comments_count, is_public) VALUES
(1,  '北京三日游攻略',      '第一天：故宫博物院深度游，感受皇家气派...',          'https://travelimg.com/note1.jpg',  1,  3200, 256, 0, TRUE),
(2,  '上海迪士尼亲子游',    '带娃游玩迪士尼的完整攻略，从早到晚不踩坑...',        'https://travelimg.com/note2.jpg',  2,  5600, 432, 0, TRUE),
(3,  '广州美食探店记',      '在广州吃遍老字号，从早茶到夜宵...',                  'https://travelimg.com/note3.jpg',  3,  2100, 189, 0, TRUE),
(4,  '成都慢生活体验',      '在成都感受巴适生活，火锅茶馆一个都不能少...',        'https://travelimg.com/note4.jpg',  5,  4500, 378, 0, TRUE),
(5,  '西安历史之旅',        '十三朝古都的深度探索，穿越千年时光...',              'https://travelimg.com/note5.jpg',  6,  3800, 312, 0, TRUE),
(6,  '杭州西湖骑行记',      '环西湖骑行30公里，每一帧都是画...',                  'https://travelimg.com/note6.jpg',  7,  5200, 456, 0, TRUE),
(7,  '重庆火锅地图',        '三天吃了八家火锅，这份重庆火锅地图请收好...',        'https://travelimg.com/note7.jpg',  8,  6800, 589, 0, TRUE),
(10, '南京梧桐大道',        '秋天的南京是最美的，梧桐大道走九遍...',              'https://travelimg.com/note8.jpg',  10, 1900, 234, 0, TRUE),
(12, '武汉樱花季',          '三月武汉，樱花如雪，东湖磨山最美赏樱地...',          'https://travelimg.com/note9.jpg',  11, 4300, 567, 0, TRUE),
(14, '厦门文艺打卡',        '鼓浪屿+沙坡尾+猫街，厦门三天文艺之旅...',           'https://travelimg.com/note10.jpg', 13, 3600, 389, 0, TRUE);

-- -------------------------------------------------------
-- 游记标签 (30条)
-- -------------------------------------------------------
INSERT INTO travel_note_tags (note_id, tag_name) VALUES
(1, '北京'), (1, '故宫'), (1, '历史文化'),
(2, '上海'), (2, '亲子'), (2, '迪士尼'),
(3, '广州'), (3, '美食'), (3, '探店'),
(4, '成都'), (4, '慢生活'), (4, '火锅'),
(5, '西安'), (5, '历史'), (5, '兵马俑'),
(6, '杭州'), (6, '西湖'), (6, '骑行'),
(7, '重庆'), (7, '火锅'), (7, '美食'),
(8, '南京'), (8, '梧桐'), (8, '秋天'),
(9, '武汉'), (9, '樱花'), (9, '东湖'),
(10,'厦门'), (10,'鼓浪屿'), (10,'文艺');

-- -------------------------------------------------------
-- 用户收藏 (15条)
-- -------------------------------------------------------
INSERT INTO user_collection (user_id, item_id, item_type, collection_type, notes, category, is_public) VALUES
(2,  1,  'route',       'collect', '北京游备用',         '国内游',   FALSE),
(3,  2,  'route',       'collect', '亲子游首选',         '亲子',     TRUE),
(1,  5,  'route',       'collect', '成都慢生活',         '休闲',     FALSE),
(2,  6,  'route',       'collect', '历史文化深度游',     '国内游',   TRUE),
(3,  8,  'route',       'collect', '文艺小清新路线',     '休闲',     FALSE),
(4,  1,  'travel_note', 'collect', '收藏攻略备用',       NULL,       FALSE),
(5,  2,  'travel_note', 'collect', '迪士尼攻略不错',     NULL,       TRUE),
(6,  4,  'travel_note', 'collect', '计划去成都',         NULL,       FALSE),
(7,  7,  'travel_note', 'collect', '火锅爱好者必看',     NULL,       TRUE),
(10, 10, 'travel_note', 'collect', '厦门旅游参考',       NULL,       FALSE),
(11, 14, 'route',       'collect', '厦门必去',           '国内游',   TRUE),
(13, 15, 'route',       'collect', '青岛啤酒节攻略',     '休闲',     TRUE),
(5,  12, 'route',       'collect', '南京文化之旅',       '国内游',   FALSE),
(9,  13, 'route',       'collect', '武汉江湖游',         '国内游',   FALSE),
(8,  9,  'route',       'collect', '重庆山城攻略',       '休闲',     TRUE);

-- -------------------------------------------------------
-- 路线评论 (15条)
-- -------------------------------------------------------
INSERT INTO route_comment (route_id, user_id, rating, content, likes_count, feedback_type) VALUES
(1,  2,  4.5, '路线规划很合理，故宫和长城都是必去景点！',                 12, 'rating'),
(1,  3,  5.0, '北京必玩路线，推荐给第一次来北京的朋友',                   8,  'rating'),
(1,  4,  NULL,'建议增加颐和园的行程，也很值得去',                          3,  'suggestion'),
(2,  4,  4.8, '带娃去迪士尼太开心了，外滩夜景也很美',                     15, 'rating'),
(2,  5,  NULL,'迪士尼排队太久，建议买快速通行证',                          2,  'suggestion'),
(3,  5,  4.0, '广州美食确实名不虚传，早茶一定要体验',                     6,  'rating'),
(4,  6,  4.6, '成都的慢生活太舒服了，火锅超级好吃',                       10, 'rating'),
(5,  7,  4.9, '兵马俑太震撼了，一定要请导游讲解',                         18, 'rating'),
(6,  8,  4.7, '西湖美景如画，灵隐寺素斋很好吃',                           9,  'rating'),
(7,  9,  4.5, '山城夜景绝了，火锅吃到停不下来',                           11, 'rating'),
(8,  10, 4.3, '拙政园太美了，苏州园林甲天下',                             7,  'rating'),
(9,  11, 3.5, '重庆的路确实魔幻，导航完全失灵',                           5,  'rating'),
(10, 12, 4.2, '中山陵很壮观，梧桐大道特别美',                             6,  'rating'),
(11, 13, 4.6, '黄鹤楼登高望远，长江大桥尽收眼底',                         8,  'rating'),
(12, 14, 4.8, '鼓浪屿太适合拍照了，文艺气息满满',                         14, 'rating');

-- -------------------------------------------------------
-- 通知 (8条)
-- -------------------------------------------------------
INSERT INTO notification (user_id, type, title, content, is_read) VALUES
(1,  'system',     '欢迎加入旅行社区',     '欢迎来到旅行社区，开始你的旅行规划吧！',              FALSE),
(2,  'comment',    '你的游记收到新评论',   '用户张三评论了你的游记《上海迪士尼亲子游》',          FALSE),
(3,  'like',       '你的路线被点赞',       '你的路线《广州美食3日游》被用户李四点赞',             FALSE),
(1,  'collection', '你的游记被收藏',       '你的游记《北京三日游攻略》被收藏了',                 TRUE),
(4,  'system',     '行程提醒',             '你收藏的成都休闲4日游有新的优惠活动',                FALSE),
(5,  'comment',    '评论被回复',           '用户王五回复了你在路线《北京经典3日游》的评论',      TRUE),
(6,  'like',       '游记获赞破百',         '恭喜！你的游记《杭州西湖骑行记》点赞数突破100',      FALSE),
(7,  'system',     '社区活动通知',         '暑期旅行日记征集活动开始了，快来参与吧！',            FALSE);

-- -------------------------------------------------------
-- 餐厅 (18条)
-- -------------------------------------------------------
INSERT INTO restaurant (name, city_id, address, latitude, longitude, rating, price_level, average_cost, cuisine_type, feature, phone, opening_hours, image_url, description) VALUES
('全聚德烤鸭店(前门店)',   1,  '北京市东城区前门大街32号',          39.8985, 116.3962, 4.6, 'high',   200.00, '北京菜', '老字号烤鸭',  '010-67011379', '11:00-21:00',              'https://restaurantimg.com/quanjude.jpg',       '百年老字号，北京烤鸭的代表'),
('大董烤鸭店(工体店)',     1,  '北京市朝阳区工人体育场东路',        39.9321, 116.4508, 4.8, 'high',   350.00, '北京菜', '意境菜',      '010-65511808', '11:00-22:00',              'https://restaurantimg.com/dadong.jpg',         '新派北京菜，烤鸭酥而不腻'),
('南翔馒头店',             2,  '上海市黄浦区豫园路85号',            31.2272, 121.4925, 4.5, 'medium', 80.00,  '上海菜', '小笼包',      '021-63554206', '7:00-20:30',               'https://restaurantimg.com/nanxiang.jpg',       '上海小笼包的代表，百年老店'),
('绿波廊',                 2,  '上海市黄浦区豫园路115号',           31.2275, 121.4920, 4.4, 'medium', 120.00, '上海菜', '本帮菜',      '021-63280602', '11:00-14:00,17:00-21:00',  'https://restaurantimg.com/lvbolang.jpg',       '豫园内的老字号，接待过众多外国元首'),
('广州酒家(文昌总店)',     3,  '广州市荔湾区文昌南路2号',          23.1175, 113.2462, 4.7, 'medium', 100.00, '粤菜',   '早茶点心',    '020-81380388', '7:00-22:00',               'https://restaurantimg.com/guangzhoujiujia.jpg','广州老字号，早茶文化代表'),
('点都德(聚福楼店)',       3,  '广州市越秀区惠福东路470号',         23.1250, 113.2680, 4.6, 'low',    60.00,  '粤菜',   '新派早茶',    '020-83190707', '8:00-22:00',               'https://restaurantimg.com/diandude.jpg',       '新派早茶，年轻人喜爱的粤式点心'),
('小龙坎火锅(春熙路店)',   5,  '成都市锦江区春熙路东段1号',         30.6578, 104.0820, 4.6, 'medium', 100.00, '川菜',   '重庆火锅',    '028-86666188', '11:00-次日2:00',           'https://restaurantimg.com/xiaolongkan.jpg',    '正宗重庆老火锅，麻辣鲜香'),
('蜀九香火锅(玉林店)',     5,  '成都市武侯区玉林南路15号',          30.6270, 104.0485, 4.5, 'medium', 90.00,  '川菜',   '成都火锅',    '028-85561388', '10:00-23:00',              'https://restaurantimg.com/shujiuxiang.jpg',    '成都本地人最爱的火锅品牌之一'),
('德发长饺子馆',           6,  '西安市碑林区钟楼广场西侧',          34.2610, 108.9420, 4.4, 'low',    50.00,  '西北菜', '饺子宴',      '029-87214060', '10:00-21:00',              'https://restaurantimg.com/defachang.jpg',      '中华老字号，饺子宴种类繁多'),
('楼外楼(孤山路店)',       7,  '杭州市西湖区孤山路30号',            30.2525, 120.1410, 4.5, 'medium', 120.00, '浙菜',   '西湖醋鱼',    '0571-87969023','10:00-21:00',              'https://restaurantimg.com/louwailou.jpg',      '西湖边的百年老店，正宗杭帮菜'),
('南京大牌档(夫子庙店)',   10, '南京市秦淮区夫子庙大石坝街48号',   32.0215, 118.7885, 4.5, 'medium', 80.00,  '苏菜',   '金陵小吃',    '025-52261777', '11:00-21:30',              'https://restaurantimg.com/nanjingdapaidang.jpg','金陵风味小吃汇集，民国风情浓郁'),
('蔡林记(户部巷店)',       11, '武汉市武昌区户部巷内',              30.5472, 114.2947, 4.3, 'low',    25.00,  '鄂菜',   '热干面',      '027-88877888', '6:00-21:00',               'https://restaurantimg.com/cailinji.jpg',       '武汉热干面鼻祖，百年老字号'),
('炊烟时代(步行街店)',     12, '长沙市天心区黄兴南路步行街内',      28.1920, 112.9730, 4.6, 'medium', 70.00,  '湘菜',   '小炒黄牛肉',  '0731-85115888','11:00-21:30',              'https://restaurantimg.com/chuiyanshidai.jpg',  '长沙人气湘菜馆，排队两小时也值得'),
('临家闽南菜(环岛路店)',   13, '厦门市思明区环岛路书法广场旁',      24.4350, 118.0980, 4.5, 'medium', 90.00,  '闽菜',   '姜母鸭',      '0592-2089876', '11:00-21:00',              'https://restaurantimg.com/linjia.jpg',         '正宗闽南风味，海景餐厅'),
('船歌鱼水饺(麦岛路店)',   14, '青岛市市南区麦岛路1号',             36.0650, 120.4250, 4.6, 'medium', 85.00,  '鲁菜',   '鱼水饺',      '0532-83881234','10:00-21:00',              'https://restaurantimg.com/chuangge.jpg',       '青岛特色鱼水饺，鲜到掉眉毛'),
('翠湖宾馆中餐厅',         15, '昆明市五华区翠湖南路6号',           25.0430, 102.7040, 4.4, 'high',   150.00, '滇菜',   '过桥米线',    '0871-65158888','11:00-14:00,17:00-21:00',  'https://restaurantimg.com/cuihu.jpg',          '正宗云南过桥米线，汤鲜味美'),
('厉家菜(金宝街店)',       1,  '北京市东城区金宝街68号',            39.9162, 116.4215, 4.9, 'high',   500.00, '北京菜', '宫廷菜',      '010-65228888', '11:30-14:00,17:30-21:30',  'https://restaurantimg.com/lijiacai.jpg',       '传奇宫廷菜，需提前一周预约'),
('桂满陇(西湖银泰店)',     7,  '杭州市上城区延安路98号西湖银泰城',  30.2460, 120.1660, 4.4, 'medium', 90.00,  '浙菜',   '杭帮菜',      '0571-87068866','11:00-14:00,17:00-21:00',  'https://restaurantimg.com/guimanlong.jpg',     '新派杭帮菜，环境古色古香');

-- -------------------------------------------------------
-- 反馈 (6条)
-- -------------------------------------------------------
INSERT INTO feedback (user_id, type, content, contact_info, status) VALUES
(1,  'suggestion', '希望增加更多城市的路线推荐',                   'zhangsan@example.com',  'resolved'),
(2,  'bug',        '路线收藏功能偶尔出现重复收藏',                 'lisi@example.com',      'processing'),
(3,  'complaint',  '景点信息更新不及时',                           'wangwu@example.com',    'pending'),
(5,  'suggestion', '建议增加景点实时拥挤度的推送通知',             'sunqi@example.com',     'pending'),
(7,  'bug',        'iOS端游记图片上传后显示方向错误',               'wujiu@example.com',     'resolved'),
(11, 'complaint',  '路线分享链接在微信中无法直接打开',             'huangyi@example.com',   'processing');

-- -------------------------------------------------------
-- 路线分享 (6条)
-- -------------------------------------------------------
INSERT INTO route_share (route_id, user_id, share_code, share_title, share_description, share_count, visit_count, is_active) VALUES
(1,  1,  'abc123def456', '北京经典3日游',   '超实用的北京旅游攻略',             25, 168, TRUE),
(2,  2,  'xyz789uvw012', '上海亲子2日游',   '带娃玩转上海迪士尼',               18,  95, TRUE),
(5,  4,  'qwe456rty789', '成都休闲4日游',   '巴适得板的成都慢生活',             32, 210, TRUE),
(8,  6,  'asd987fgh654', '杭州诗意2日游',   '西湖+灵隐寺，江南最美路线',        15,  78, TRUE),
(11, 12, 'zxc321vbn890', '武汉江湖3日游',   '黄鹤楼+东湖，江城韵味十足',         8,  42, TRUE),
(14, 14, 'poi098mkl765', '厦门鼓浪屿2日游', '文艺小清新必打卡路线',             22, 156, TRUE);

-- -------------------------------------------------------
-- 资源文件 (6条)
-- -------------------------------------------------------
INSERT INTO resource_file (file_name, file_path, file_size, file_type, upload_user_id, description, status, download_count, comment_count, rating, view_count, file_category, version) VALUES
('北京旅游攻略.pdf',       '/files/beijing-guide.pdf',    2048576, 'pdf',  1,  '北京3日游详细攻略',              1, 125, 8,  4.5, 520, '攻略文档', 1),
('上海迪士尼地图.jpg',    '/files/disney-map.jpg',       1048576, 'jpg',  2,  '上海迪士尼乐园高清地图',         1, 256, 12, 4.8, 890, '地图',     1),
('成都美食推荐.xlsx',     '/files/chengdu-food.xlsx',    512000,  'xlsx', 4,  '成都火锅及小吃推荐清单',         1,  89, 5,  4.3, 340, '攻略文档', 1),
('西安兵马俑导览图.pdf',  '/files/xian-terracotta.pdf',  3145728, 'pdf',  5,  '兵马俑博物馆官方导览及历史介绍',   1, 198, 15, 4.7, 670, '攻略文档', 1),
('南京赏秋路线图.jpg',    '/files/nanjing-autumn.jpg',   2097152, 'jpg',  10, '南京秋季梧桐大道及赏秋路线',      1,  67, 3,  4.6, 230, '地图',     1),
('重庆火锅排行榜.xlsx',   '/files/chongqing-hotpot.xlsx',256000,  'xlsx', 7,  '重庆本地人推荐的火锅店排行榜',   1, 156, 9,  4.8, 450, '攻略文档', 1);

-- -------------------------------------------------------
-- 文件标签 (12条)
-- -------------------------------------------------------
INSERT INTO file_tag (tag_name, tag_type, file_id, user_id, usage_count) VALUES
('攻略',   'category', 1, 1,  15),
('北京',   'location', 1, 1,  10),
('地图',   'category', 2, 2,  8),
('上海',   'location', 2, 2,  12),
('美食',   'category', 3, 4,  6),
('成都',   'location', 3, 4,  9),
('历史',   'category', 4, 5,  11),
('西安',   'location', 4, 5,  14),
('秋天',   'theme',    5, 10, 6),
('南京',   'location', 5, 10, 8),
('火锅',   'category', 6, 7,  18),
('重庆',   'location', 6, 7,  20);

-- -------------------------------------------------------
-- 文件评论 (8条)
-- -------------------------------------------------------
INSERT INTO file_comment (file_id, user_id, user_name, content, rating, likes, status) VALUES
(1, 2,  'lisi',     '攻略很详细，非常实用！',               5, 12, 1),
(1, 3,  'wangwu',   '建议增加住宿推荐部分',                 4,  5, 1),
(2, 1,  'zhangsan', '地图清晰度很高，帮了大忙',             5,  8, 1),
(3, 2,  'lisi',     '火锅推荐很地道，收藏了',               5,  3, 1),
(4, 8,  'zhengshi', '兵马俑资料非常详细，涨知识了',         5,  7, 1),
(4, 6,  'zhouba',   '如果能加入秦始皇陵的介绍就更好了',     4,  2, 1),
(5, 11, 'huangyi',  '南京秋天真的太美了，照片拍得好',       5,  4, 1),
(6, 9,  'chenshiyi','去了其中三家，确实不踩雷，强推！',     5, 6,  1);

-- -------------------------------------------------------
-- UI字典 (18条)
-- -------------------------------------------------------
INSERT INTO ui_dictionary (dict_type, dict_key, dict_value, dict_label, sort_order) VALUES
('difficulty',     'easy',      '简单',     '简单',       1),
('difficulty',     'medium',    '中等',     '中等',       2),
('difficulty',     'hard',      '困难',     '困难',       3),
('feedback_type',  'rating',    '评分',     '评分',       1),
('feedback_type',  'suggestion','建议',     '建议',       2),
('feedback_type',  'complaint', '投诉',     '投诉',       3),
('notify_type',    'system',    '系统',     '系统通知',   1),
('notify_type',    'comment',   '评论',     '评论通知',   2),
('notify_type',    'like',      '点赞',     '点赞通知',   3),
('notify_type',    'collection','收藏',     '收藏通知',   4),
('crowd_level',    '1',         '空闲',     '人少',       1),
('crowd_level',    '2',         '较少',     '较少',       2),
('crowd_level',    '3',         '适中',     '适中',       3),
('crowd_level',    '4',         '较多',     '拥挤',       4),
('crowd_level',    '5',         '饱和',     '爆满',       5),
('price_level',    'low',       '实惠',     '人均<80',    1),
('price_level',    'medium',    '中等',     '人均80-200', 2),
('price_level',    'high',      '高端',     '人均>200',   3);

