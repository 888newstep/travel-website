-- 路线规划只保留有效的同城景点关系，交通距离、时长、路况和过路费运行时从高德 API 获取。
-- 本脚本可重复执行：已有关系会更新游览顺序，不会重复插入。

START TRANSACTION;

-- 固定交通种子不能代表实时交通，历史数据统一清空。
DELETE FROM route_transport;

-- 清理历史初始化脚本产生的跨城市关系。
DELETE route_attraction
FROM route_attractions route_attraction
JOIN route ON route.id = route_attraction.route_id
JOIN attraction ON attraction.id = route_attraction.attraction_id
WHERE route.city_id <> attraction.city_id;

DROP TEMPORARY TABLE IF EXISTS expected_route_attraction;
CREATE TEMPORARY TABLE expected_route_attraction (
    route_title     VARCHAR(100) NOT NULL,
    attraction_name VARCHAR(100) NOT NULL,
    day_number      INT NOT NULL,
    visit_order     INT NOT NULL,
    notes           VARCHAR(255) NOT NULL,
    PRIMARY KEY (route_title, attraction_name)
);

INSERT INTO expected_route_attraction
    (route_title, attraction_name, day_number, visit_order, notes)
VALUES
    ('北京经典3日游',   '故宫博物院',     1, 1, '早上8点入园，避开人流'),
    ('北京经典3日游',   '八达岭长城',     2, 1, '建议乘坐缆车上下山'),
    ('北京经典3日游',   '天坛公园',       3, 1, '天坛回音壁体验'),
    ('北京深度5日游',   '故宫博物院',     1, 1, '故宫深度游，预留4小时'),
    ('北京深度5日游',   '天坛公园',       2, 1, '天坛公园晨练体验'),
    ('北京深度5日游',   '八达岭长城',     3, 1, '长城一日游，带好干粮'),
    ('上海亲子2日游',   '上海迪士尼乐园', 1, 1, '提前下载迪士尼APP，绑定门票'),
    ('上海亲子2日游',   '外滩',           2, 1, '晚上7点看外滩灯光秀'),
    ('广州美食3日游',   '广州塔',         1, 1, '登塔最佳时间18:00-20:00'),
    ('成都休闲4日游',   '宽窄巷子',       1, 1, '推荐品尝巷子里的地道火锅'),
    ('成都休闲4日游',   '都江堰',         2, 1, '都江堰建议请导游讲解水利原理'),
    ('西安历史5日游',   '兵马俑博物馆',   1, 1, '请专业导游讲解，体验更佳'),
    ('西安历史5日游',   '大雁塔',         2, 1, '大雁塔音乐喷泉晚上看最美'),
    ('西安经典3日游',   '兵马俑博物馆',   1, 1, '兵马俑和华清宫适合安排一日游'),
    ('西安经典3日游',   '大雁塔',         2, 1, '大雁塔和大唐不夜城适合傍晚游览'),
    ('杭州诗意2日游',   '西湖',           1, 1, '租一艘小船游西湖，别有韵味'),
    ('杭州诗意2日游',   '灵隐寺',         2, 1, '灵隐寺祈福，品尝素斋'),
    ('重庆山城3日游',   '洪崖洞',         1, 1, '晚上21:00后拍照效果最好'),
    ('苏州园林2日游',   '拙政园',         1, 1, '拙政园建议早上去，人少清静'),
    ('南京民国2日游',   '中山陵',         1, 1, '中山陵392级台阶，合理安排体力'),
    ('武汉江湖3日游',   '黄鹤楼',         1, 1, '黄鹤楼上可远眺长江大桥'),
    ('厦门鼓浪屿2日游', '鼓浪屿',         1, 1, '鼓浪屿船票需提前网上预订'),
    ('青岛海滨3日游',   '崂山',           1, 1, '崂山建议乘坐索道上山');

-- 同名景点存在历史重复数据时，稳定选择最早的一条，避免笛卡尔积插入。
INSERT INTO route_attractions
    (route_id, attraction_id, day_number, visit_order, notes)
SELECT
    route.id,
    canonical_attraction.attraction_id,
    expected.day_number,
    expected.visit_order,
    expected.notes
FROM expected_route_attraction expected
JOIN route ON route.title = expected.route_title
JOIN (
    SELECT name, city_id, MIN(id) AS attraction_id
    FROM attraction
    GROUP BY name, city_id
) canonical_attraction
  ON canonical_attraction.name = expected.attraction_name
 AND canonical_attraction.city_id = route.city_id
ON DUPLICATE KEY UPDATE
    day_number = VALUES(day_number),
    visit_order = VALUES(visit_order),
    notes = VALUES(notes);

DROP TEMPORARY TABLE expected_route_attraction;

COMMIT;

-- 验收结果：跨城市关系和固定交通数据均应为 0。
SELECT COUNT(*) AS cross_city_relation_count
FROM route_attractions route_attraction
JOIN route ON route.id = route_attraction.route_id
JOIN attraction ON attraction.id = route_attraction.attraction_id
WHERE route.city_id <> attraction.city_id;

SELECT COUNT(*) AS route_transport_count
FROM route_transport;

-- 高德实测至少需要一条恰好 2 点路线和一条至少 3 点路线。
SELECT route.id, route.title, COUNT(*) AS point_count
FROM route
JOIN route_attractions route_attraction ON route_attraction.route_id = route.id
WHERE route.title IN ('上海亲子2日游', '北京经典3日游', '北京深度5日游')
GROUP BY route.id, route.title
ORDER BY route.id;
