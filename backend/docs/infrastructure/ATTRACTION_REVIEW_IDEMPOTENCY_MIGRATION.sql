-- 每位用户对同一景点只保留一条点评，重复提交转换为更新。
-- 先保留最新记录，再幂等创建唯一索引。
CREATE TABLE IF NOT EXISTS attraction_review (
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '点评ID',
    attraction_id   INT NOT NULL COMMENT '景点ID',
    user_id         INT NOT NULL COMMENT '用户ID',
    rating          TINYINT NOT NULL DEFAULT 5 COMMENT '评分(1-5)',
    content         TEXT COMMENT '点评内容',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_attraction (attraction_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating DESC)
) COMMENT='景点点评表' ENGINE=InnoDB;

DELETE older_review
FROM attraction_review older_review
JOIN attraction_review newer_review
  ON newer_review.attraction_id = older_review.attraction_id
 AND newer_review.user_id = older_review.user_id
 AND newer_review.id > older_review.id;

SET @schema_name = DATABASE();
SET @add_review_unique_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'attraction_review'
          AND index_name = 'uk_attraction_user'
    ),
    'SELECT 1',
    'ALTER TABLE attraction_review ADD UNIQUE KEY uk_attraction_user (attraction_id, user_id)'
);

PREPARE add_review_unique_index_statement FROM @add_review_unique_index;
EXECUTE add_review_unique_index_statement;
DEALLOCATE PREPARE add_review_unique_index_statement;
