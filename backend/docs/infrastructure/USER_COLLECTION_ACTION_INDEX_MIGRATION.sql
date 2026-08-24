-- 允许同一用户对同一篇游记同时执行点赞和收藏。
-- 脚本可重复执行，适用于 MySQL 8.x。

SET @schema_name = DATABASE();

UPDATE user_collection
SET collection_type = 'collect'
WHERE collection_type IS NULL OR TRIM(collection_type) = '';

ALTER TABLE user_collection
    MODIFY COLUMN collection_type VARCHAR(20) NOT NULL DEFAULT 'collect'
    COMMENT '收藏方式: collect/like';

SET @drop_old_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'user_collection'
          AND index_name = 'uk_user_item'
    ),
    'ALTER TABLE user_collection DROP INDEX uk_user_item',
    'SELECT 1'
);
PREPARE drop_old_index_statement FROM @drop_old_index;
EXECUTE drop_old_index_statement;
DEALLOCATE PREPARE drop_old_index_statement;

SET @add_new_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'user_collection'
          AND index_name = 'uk_user_item_action'
    ),
    'SELECT 1',
    'ALTER TABLE user_collection ADD UNIQUE KEY uk_user_item_action (user_id, item_id, item_type, collection_type)'
);
PREPARE add_new_index_statement FROM @add_new_index;
EXECUTE add_new_index_statement;
DEALLOCATE PREPARE add_new_index_statement;
