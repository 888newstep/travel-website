-- 路线评论查询与点赞一致性迁移。
-- 可重复执行，适用于 MySQL 8.x。

SET @schema_name = DATABASE();

UPDATE route_comment
SET likes_count = 0
WHERE likes_count IS NULL OR likes_count < 0;

ALTER TABLE route_comment
    MODIFY COLUMN likes_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    MODIFY COLUMN rating DECIMAL(3,2) NULL DEFAULT NULL COMMENT '综合评分(1-5)';

ALTER TABLE user_collection
    MODIFY COLUMN item_type VARCHAR(20) NOT NULL
    COMMENT '类型: route/travel_note/route_comment';

SET @add_user_item_action_index = IF(
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
PREPARE add_user_item_action_index_statement FROM @add_user_item_action_index;
EXECUTE add_user_item_action_index_statement;
DEALLOCATE PREPARE add_user_item_action_index_statement;

SET @add_route_feed_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'route_comment'
          AND index_name = 'idx_route_published_reply_created'
    ),
    'SELECT 1',
    'ALTER TABLE route_comment ADD INDEX idx_route_published_reply_created (route_id, is_published, reply_to, created_at)'
);
PREPARE add_route_feed_index_statement FROM @add_route_feed_index;
EXECUTE add_route_feed_index_statement;
DEALLOCATE PREPARE add_route_feed_index_statement;

SET @add_user_feed_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'route_comment'
          AND index_name = 'idx_user_published_created'
    ),
    'SELECT 1',
    'ALTER TABLE route_comment ADD INDEX idx_user_published_created (user_id, is_published, created_at)'
);
PREPARE add_user_feed_index_statement FROM @add_user_feed_index;
EXECUTE add_user_feed_index_statement;
DEALLOCATE PREPARE add_user_feed_index_statement;
