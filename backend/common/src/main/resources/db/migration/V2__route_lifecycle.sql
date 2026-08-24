-- 现有 travel_website 数据库的路线生命周期迁移。
-- 新环境应优先执行统一 init_complete.sql；已有环境执行本脚本一次即可。

SET @route_status_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'route'
      AND column_name = 'status'
);
SET @route_status_sql = IF(
    @route_status_exists = 0,
    'ALTER TABLE route ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''DRAFT'' COMMENT ''路线状态'' AFTER is_public',
    'SELECT 1'
);
PREPARE route_status_stmt FROM @route_status_sql;
EXECUTE route_status_stmt;
DEALLOCATE PREPARE route_status_stmt;

SET @route_version_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'route'
      AND column_name = 'version'
);
SET @route_version_sql = IF(
    @route_version_exists = 0,
    'ALTER TABLE route ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本'' AFTER status',
    'SELECT 1'
);
PREPARE route_version_stmt FROM @route_version_sql;
EXECUTE route_version_stmt;
DEALLOCATE PREPARE route_version_stmt;

UPDATE route
SET status = CASE WHEN is_public = TRUE THEN 'PUBLISHED' ELSE 'DRAFT' END
WHERE status IS NULL OR status = '' OR status = 'DRAFT';

SET @route_status_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'route'
      AND index_name = 'idx_status_public_city'
);
SET @route_status_index_sql = IF(
    @route_status_index_exists = 0,
    'ALTER TABLE route ADD INDEX idx_status_public_city (status, is_public, city_id)',
    'SELECT 1'
);
PREPARE route_status_index_stmt FROM @route_status_index_sql;
EXECUTE route_status_index_stmt;
DEALLOCATE PREPARE route_status_index_stmt;
