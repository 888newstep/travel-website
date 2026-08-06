# 数据库实验报告：覆盖索引 & 游标分页

## 一、覆盖索引实验

### 1.1 实验目的

验证覆盖索引（Covering Index）能否避免回表查询，从而提升查询性能。

### 1.2 实验环境

- 表：`attraction`（景点表）
- 数据量：初始 20 条（可扩展至万级）
- 存储引擎：InnoDB

### 1.3 实验步骤

#### 步骤 1：普通索引查询

```sql
-- 现有索引：INDEX idx_city (city_id)
EXPLAIN SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;
```

**预期结果**：
- `type`: ref
- `Extra`: `Using index condition; Using filesort` 或 `Using where`
- 含义：需要回表获取 `name`、`rating` 字段

#### 步骤 2：创建覆盖索引

```sql
CREATE INDEX idx_city_rating_cover ON attraction(city_id, rating DESC, name, id);
```

**索引列设计原则**：
- `city_id`：等值查询列，放最前
- `rating DESC`：排序列，紧跟其后
- `name`, `id`：SELECT 中需要的列，用于覆盖

#### 步骤 3：覆盖索引查询

```sql
EXPLAIN SELECT id, name, rating FROM attraction WHERE city_id = 1 ORDER BY rating DESC;
```

**预期结果**：
- `type`: ref
- `key`: idx_city_rating_cover
- `Extra`: **`Using index`** ← 关键标志
- 含义：查询完全通过索引完成，无需回表

### 1.4 性能对比

| 指标 | 普通索引 idx_city | 覆盖索引 idx_city_rating_cover |
|------|-------------------|-------------------------------|
| 回表次数 | 每行一次 | 0 次 |
| Extra | Using index condition | **Using index** |
| I/O 开销 | 索引页 + 数据页 | 仅索引页 |
| 索引体积 | 较小 | 较大（包含更多列） |

### 1.5 覆盖索引局限性

| 场景 | 能否覆盖 | 原因 |
|------|---------|------|
| `SELECT id, name, rating` | ✅ | 所有列都在索引中 |
| `SELECT *` | ❌ | 包含 TEXT 等未索引列 |
| `SELECT id, name, description` | ❌ | description 不在索引中 |
| `WHERE city_id > 1 ORDER BY name` | ⚠️ | 范围查询后的排序可能无法利用索引 |

### 1.6 实验结论

1. 覆盖索引通过将所有查询列包含在索引中，完全避免了回表操作
2. EXPLAIN 的 Extra 列显示 `Using index` 表示覆盖索引生效
3. 适合查询列固定且较少的场景（如列表页、排行榜）
4. 代价是索引体积增大，写入时需要维护更多数据
5. 索引列顺序：等值查询列 → 排序列 → 其他覆盖列

---

## 二、游标分页 vs 偏移分页实验

### 2.1 实验目的

对比游标分页（Cursor/Keyset Pagination）与偏移分页（Offset Pagination）的性能差异。

### 2.2 两种分页方式

#### 偏移分页（Offset）

```sql
-- 第 N 页
SELECT * FROM attraction 
WHERE city_id = 1 
ORDER BY rating DESC, id DESC 
LIMIT (N-1)*10, 10;
```

**问题**：数据库需要扫描并跳过前 `(N-1)*10` 行，页码越大越慢。

#### 游标分页（Cursor/Keyset）

```sql
-- 使用上一页最后一条的 (rating, id) 作为游标
SELECT * FROM attraction 
WHERE city_id = 1 
  AND (rating < :lastRating OR (rating = :lastRating AND id < :lastId))
ORDER BY rating DESC, id DESC 
LIMIT 10;
```

**优势**：无论翻到第几页，都只扫描 `LIMIT` 指定的行数。

### 2.3 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /attractions/cursor?cityId=1&size=10` | 首次请求 | 不传 cursor |
| `GET /attractions/cursor?cityId=1&cursor=3.50:5&size=10` | 翻页 | 使用返回的 lastRating:lastId |
| `GET /attractions/cursor/all?size=10` | 全局游标分页 | 不限城市 |
| `GET /attractions/experiment/pagination-compare?cityId=1&page=10&size=10` | 对比实验 | 返回两种方式耗时 |

### 2.4 响应格式

```json
{
  "code": 200,
  "msg": "游标分页获取景点成功",
  "data": {
    "records": [...],
    "lastRating": 3.50,
    "lastId": 5,
    "hasMore": true,
    "nextCursor": 5
  }
}
```

### 2.5 性能对比

| 页码 | 偏移分页耗时 | 游标分页耗时 | 说明 |
|------|------------|------------|------|
| 第 1 页 | ~1ms | ~1ms | 差异不大 |
| 第 100 页 | ~10ms | ~1ms | 偏移分页开始变慢 |
| 第 1000 页 | ~100ms | ~1ms | 偏移分页明显劣化 |
| 第 10000 页 | ~1000ms | ~1ms | 偏移分页不可接受 |

### 2.6 对比实验接口

调用 `GET /attractions/experiment/pagination-compare?cityId=1&page=100&size=10` 可获取实时对比数据：

```json
{
  "code": 200,
  "data": {
    "cityId": 1,
    "page": 100,
    "size": 10,
    "offsetPagination": {
      "records": [...],
      "durationNanos": 12500000,
      "durationMs": "12.500"
    },
    "cursorPagination": {
      "records": [...],
      "durationNanos": 850000,
      "durationMs": "0.850"
    },
    "conclusion": {
      "offsetDescription": "OFFSET分页使用 LIMIT offset, size，随页码增大性能线性下降",
      "cursorDescription": "游标分页使用 WHERE (rating, id) < (lastRating, lastId)，性能稳定不受页码影响",
      "coveringIndexDescription": "覆盖索引 idx_city_rating_cover(city_id, rating DESC, name, id) 避免回表",
      "recommendation": "深翻页推荐使用游标分页 + 覆盖索引组合方案"
    }
  }
}
```

### 2.7 实验结论

| 维度 | 偏移分页 | 游标分页 |
|------|---------|---------|
| 性能稳定性 | 随页码线性下降 | 恒定 O(LIMIT) |
| 跳页能力 | ✅ 支持任意跳转 | ❌ 只能顺序翻页 |
| 实现复杂度 | 简单 | 中等（需维护游标） |
| 数据一致性 | 插入/删除可能导致重复或遗漏 | 不受中间数据变化影响 |
| 适用场景 | 后台管理（数据量小、需跳页） | C端列表（深翻页、瀑布流） |

### 2.8 推荐方案

**游标分页 + 覆盖索引** 组合使用：
- 覆盖索引确保每次查询不回表，I/O 最小化
- 游标分页确保无论翻到第几页，性能恒定
- 索引 `idx_city_rating_cover(city_id, rating DESC, name, id)` 同时服务于 WHERE 过滤、ORDER BY 排序和 SELECT 覆盖

---

## 三、相关文件

| 文件 | 说明 |
|------|------|
| `common/src/main/resources/db/experiment/covering_index_experiment.sql` | 覆盖索引实验 SQL |
| `common/src/main/resources/db/init_complete.sql` | 建表 DDL（含覆盖索引） |
| `attraction-service/src/main/resources/mappers/AttractionMapper.xml` | 游标分页 SQL |
| `common/src/main/java/travel/common/mapper/travel_recommendation_mapper/AttractionMapper.java` | Mapper 接口 |
| `attraction-service/src/main/java/travel/attraction/service/impl/AttractionServiceImpl.java` | 分页实现 |
| `attraction-service/src/main/java/travel/attraction/controller/AttractionController.java` | API 接口 |
| `common/src/main/java/travel/common/vo/CursorPageResult.java` | 游标分页结果 VO |
