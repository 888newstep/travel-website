-- ============================================================
-- 景点排序索引与窄投影覆盖索引实验
-- ============================================================
-- 目的：
-- 1. 验证 idx_city_rating_id 是否消除城市游标查询的额外排序；
-- 2. 验证 idx_rating_id 是否支持全局游标查询的稳定排序；
-- 3. 通过窄投影说明“排序索引”和“覆盖索引”的边界。
--
-- 重要说明：
-- attraction 查询通常返回完整实体，包含 description、images 等大字段，
-- 因此 idx_city_rating_id / idx_rating_id 不是完整实体查询的覆盖索引，
-- 主要价值是减少 Using filesort，并为游标分页提供稳定的索引顺序。
-- 本脚本只读验证为主，不删除、不重建线上已有索引。
-- 适用版本：MySQL 8.0+
-- ============================================================

-- ============================================================
-- 实验1：数据量、现有索引与统计信息
-- ============================================================

SELECT COUNT(*) AS total_attractions FROM attraction;
SHOW INDEX FROM attraction;

-- 迁移后建议执行一次，避免统计信息滞后影响 EXPLAIN 选择。
ANALYZE TABLE attraction;

-- ============================================================
-- 实验2：城市游标查询的排序索引验证
-- ============================================================

-- 2.1 应用实际查询：等值过滤 + rating/id 稳定降序 + 首屏取 11 条。
-- 迁移前重点观察：key 通常为 idx_city，Extra 可能包含 Using filesort。
EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 2.2 迁移后重新执行同一条 EXPLAIN。
-- 预期：key 可能选择 idx_city_rating_id；即使仍需回表获取大字段，
-- 也应优先验证是否消除了排序开销，不能仅凭 key 名称判断结果。
EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 2.3 强制索引做诊断对比，不作为线上固定 Hint。
-- 只有在两个索引都存在时执行；执行前先确认 SHOW INDEX 结果。
EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction FORCE INDEX (idx_city)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction FORCE INDEX (idx_city_rating_id)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 2.4 验证游标条件仍能利用联合索引的最左匹配。
-- 参数示例含义：只取 rating/id 游标之前的记录。
EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction
WHERE city_id = 1
  AND (rating < 4.90 OR (rating = 4.90 AND id < 171))
ORDER BY rating DESC, id DESC
LIMIT 11;

-- ============================================================
-- 实验3：全局游标查询的排序索引验证
-- ============================================================

EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction
ORDER BY rating DESC, id DESC
LIMIT 11;

EXPLAIN
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction
WHERE rating < 4.90 OR (rating = 4.90 AND id < 171)
ORDER BY rating DESC, id DESC
LIMIT 11;

-- ============================================================
-- 实验4：窄投影覆盖索引边界
-- ============================================================

-- 4.1 该投影只读取索引中已有的 city_id、rating 和隐含主键 id，
-- 可能出现 Extra=Using index，说明这是“窄投影覆盖”，不是实体查询覆盖。
EXPLAIN
SELECT id, city_id, rating
FROM attraction FORCE INDEX (idx_city_rating_id)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 4.2 加入 name 后需要回表，因为 name 不在 idx_city_rating_id 中。
EXPLAIN
SELECT id, name, city_id, rating
FROM attraction FORCE INDEX (idx_city_rating_id)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 4.3 加入 TEXT/大字段后更不可能是覆盖索引。
EXPLAIN
SELECT id, name, description, images, city_id, rating
FROM attraction FORCE INDEX (idx_city_rating_id)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- ============================================================
-- 实验5：可选的耗时对比
-- ============================================================
-- 优先使用 MySQL 8.0.18+ 的 EXPLAIN ANALYZE。它会实际执行查询，
-- 仅在测试窗口使用，避免在生产高峰运行。

EXPLAIN ANALYZE
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction FORCE INDEX (idx_city)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

EXPLAIN ANALYZE
SELECT id, name, city_id, address, description, ticket_price,
       opening_hours, latitude, longitude, images, rating, view_count,
       created_at, updated_at
FROM attraction FORCE INDEX (idx_city_rating_id)
WHERE city_id = 1
ORDER BY rating DESC, id DESC
LIMIT 11;

-- 若当前 MySQL 版本不支持 EXPLAIN ANALYZE，改用普通 EXPLAIN，
-- 并用 JMeter 的同一场景对比平均 RT/P95/P99，不使用 RESET QUERY CACHE。

-- ============================================================
-- 实验6：写入成本与回滚边界
-- ============================================================

SHOW TABLE STATUS LIKE 'attraction';
SHOW INDEX FROM attraction;

-- 索引会增加 INSERT/UPDATE 的维护成本。不要在此处直接 DROP 线上索引；
-- 如确需回滚，先确认没有依赖该索引的压测和业务窗口，再单独执行：
-- ALTER TABLE attraction DROP INDEX idx_city_rating_id;
-- ALTER TABLE attraction DROP INDEX idx_rating_id;

-- ============================================================
-- 实验结论记录模板
-- ============================================================
-- 1. 城市查询：记录迁移前后 key、rows、Extra，重点观察 Using filesort。
-- 2. 全局查询：记录 idx_rating_id 是否被选择，以及深游标条件下的 rows。
-- 3. 覆盖边界：只有窄投影可能出现 Using index，完整 Attraction 查询仍需回表。
-- 4. 性能结论：以同一数据集、同一预热和 JMeter 场景的 P95/P99 为准。
-- 5. 写入成本：记录索引数量、表大小及写入压测变化，不能只看读延迟。
-- ============================================================
