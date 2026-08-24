-- 路线景点顺序一致性迁移。
-- 先规范历史数据，再补齐非空列和同日顺序唯一约束；适用于 MySQL 8.x。

SET @schema_name = DATABASE();

SET @drop_route_day_order_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'route_attractions'
          AND index_name = 'uk_route_day_visit_order'
    ),
    'ALTER TABLE route_attractions DROP INDEX uk_route_day_visit_order',
    'SELECT 1'
);
PREPARE drop_route_day_order_index_statement FROM @drop_route_day_order_index;
EXECUTE drop_route_day_order_index_statement;
DEALLOCATE PREPARE drop_route_day_order_index_statement;

UPDATE route_attractions relation_row
JOIN (
    SELECT
        id,
        normalized_day,
        ROW_NUMBER() OVER (
            PARTITION BY route_id, normalized_day
            ORDER BY valid_order_rank, visit_order, id
        ) AS normalized_order
    FROM (
        SELECT
            id,
            route_id,
            visit_order,
            CASE
                WHEN day_number IS NULL OR day_number <= 0 THEN 1
                ELSE day_number
            END AS normalized_day,
            CASE
                WHEN visit_order IS NULL OR visit_order <= 0 THEN 1
                ELSE 0
            END AS valid_order_rank
        FROM route_attractions
    ) source_rows
) ranked_rows ON ranked_rows.id = relation_row.id
SET
    relation_row.day_number = ranked_rows.normalized_day,
    relation_row.visit_order = ranked_rows.normalized_order;

ALTER TABLE route_attractions
    MODIFY COLUMN day_number INT NOT NULL DEFAULT 1 COMMENT '第几天',
    MODIFY COLUMN visit_order INT NOT NULL COMMENT '游览顺序';

ALTER TABLE route_attractions
    ADD UNIQUE KEY uk_route_day_visit_order (route_id, day_number, visit_order);
