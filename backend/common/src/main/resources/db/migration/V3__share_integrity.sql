-- 分享表允许 note/file 类型不绑定 route_id。
SET @route_share_route_nullable_sql = IF(
    (SELECT IS_NULLABLE
       FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'route_share'
        AND column_name = 'route_id') = 'NO',
    'ALTER TABLE route_share MODIFY COLUMN route_id INT NULL COMMENT ''路线ID，路线分享以外的类型可为空''',
    'SELECT 1'
);
PREPARE route_share_route_nullable_stmt FROM @route_share_route_nullable_sql;
EXECUTE route_share_route_nullable_stmt;
DEALLOCATE PREPARE route_share_route_nullable_stmt;
