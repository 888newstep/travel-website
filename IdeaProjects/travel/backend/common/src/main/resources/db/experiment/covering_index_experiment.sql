-- ============================================================
-- 覆盖索引实验 (Covering Index Experiment)
-- 目的：验证覆盖索引可以避免回表查询，提升查询性能
-- ============================================================

-- ============================================================
-- 实验1：准备数据（确保有足够的测试数据）
-- ============================================================

-- 查看 attraction 表现有数据量
SELECT COUNT(*) AS total_attractions FROM attraction;

-- 如果数据量不够，可以批量插入测试数据（可选）
-- INSERT INTO attraction (name, city_id, address, rating, view_count, latitude, longitude)
-- SELECT 
--     CONCAT('测试景点_', seq), 
--     (seq % 20) + 1,
--     CONCAT('测试地址_', seq),
--     ROUND(RAND() * 5, 2),
--     FLOOR(RAND() * 10000),
--     ROUND(30 + RAND() * 10, 8),
--     ROUND(100 + RAND() * 20, 8)
-- FROM (
--     SELECT @row := @row + 1 AS seq
--     FROM attraction, (SELECT @row := 0) r
--     LIMIT 10000
-- ) tmp;

-- ============================================================
-- 实验2：对比普通索引 vs 覆盖索引
-- ============================================================

-- -------------------------------------------------------
-- 2.1 场景：按城市查询景点，返回 id、name、rating
-- -------------------------------------------------------

-- 现有索引（非覆盖索引）
-- INDEX idx_city (city_id)
-- 执行查询并查看执行计划
EXPLAIN SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;

-- 分析：Extra 列会显示 "Using index condition" 或 "Using filesort"
-- 这意味着需要回表获取 name、rating 字段

-- -------------------------------------------------------
-- 2.2 创建覆盖索引
-- -------------------------------------------------------

-- 创建覆盖索引：将查询需要的所有列都包含在索引中
-- 索引列顺序：city_id(等值查询) -> rating(排序) -> name(覆盖) -> id(覆盖)
CREATE INDEX idx_city_covering ON attraction(city_id, rating DESC, name, id);

-- -------------------------------------------------------
-- 2.3 使用覆盖索引后再次查看执行计划
-- -------------------------------------------------------

EXPLAIN SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;

-- 分析：Extra 列应显示 "Using index"
-- 这表示查询完全通过索引完成，无需回表

-- ============================================================
-- 实验3：性能对比（使用 profiling 量化差异）
-- ============================================================

-- 开启 profiling
SET profiling = 1;

-- 清除查询缓存，确保公平对比
RESET QUERY CACHE;

-- 3.1 使用普通索引 idx_city（强制使用）
SELECT id, name, rating FROM attraction 
WHERE city_id = 1 ORDER BY rating DESC;
-- 注意执行时间

-- 3.2 使用覆盖索引 idx_city_covering（强制使用）
SELECT id, name, rating FROM attraction 
WHERE city_id = 1 ORDER BY rating DESC;
-- 注意执行时间

-- 查看 profiling 结果
SHOW PROFILES;

-- 关闭 profiling
SET profiling = 0;

-- ============================================================
-- 实验4：EXPLAIN FORMAT=JSON 详细分析
-- ============================================================

-- 4.1 普通索引的执行计划（先删除覆盖索引对比）
DROP INDEX idx_city_covering ON attraction;

EXPLAIN FORMAT=JSON 
SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;

-- 关注：
-- "cost_info" 中的 query_cost
-- "attached_condition" 表示需要回表过滤

-- 4.2 恢复覆盖索引
CREATE INDEX idx_city_covering ON attraction(city_id, rating DESC, name, id);

EXPLAIN FORMAT=JSON 
SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;

-- 关注：
-- "cost_info" 中的 query_cost 应明显降低
-- "using_index": true 表示覆盖索引生效
-- 无 "attached_condition" 或非常轻量

-- ============================================================
-- 实验5：覆盖索引的局限性验证
-- ============================================================

-- 5.1 查询包含不在索引中的列 → 无法覆盖
EXPLAIN SELECT id, name, rating, description 
FROM attraction WHERE city_id = 1 ORDER BY rating DESC;
-- Extra 不会显示 "Using index"，因为 description 不在索引中

-- 5.2 SELECT * 无法使用覆盖索引
EXPLAIN SELECT * FROM attraction WHERE city_id = 1 ORDER BY rating DESC;
-- Extra 不会显示 "Using index"

-- 5.3 范围查询后排序可能破坏覆盖索引效率
EXPLAIN SELECT id, name, rating 
FROM attraction WHERE city_id = 1 AND rating > 3.0 ORDER BY name;
-- 注意：范围查询(city_id)后的排序(name)可能无法利用索引排序

-- ============================================================
-- 实验6：清理（可选）
-- ============================================================

-- 如果不需要保留覆盖索引，可以删除
-- DROP INDEX idx_city_covering ON attraction;

-- ============================================================
-- 实验结论
-- ============================================================
-- 1. 覆盖索引通过将所有查询列包含在索引中，避免了回表操作
-- 2. EXPLAIN 的 Extra 列显示 "Using index" 表示覆盖索引生效
-- 3. 覆盖索引适合查询列固定且较少的场景（如列表页）
-- 4. 覆盖索引的代价是索引体积增大，写入时需要维护更多数据
-- 5. SELECT * 或查询 TEXT/BLOB 字段时无法使用覆盖索引
-- 6. 索引列顺序设计：等值查询列在前，范围/排序列在后
-- ============================================================
