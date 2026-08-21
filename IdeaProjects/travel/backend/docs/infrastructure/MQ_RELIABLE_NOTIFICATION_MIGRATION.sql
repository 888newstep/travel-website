-- Reliable notification consumer idempotency migration for an existing database.
-- The script is safe to execute repeatedly after taking a database backup.

SET @schema_name = DATABASE();

SET @add_source_message_id = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'notification'
          AND column_name = 'source_message_id'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN source_message_id VARCHAR(100) NULL COMMENT ''RabbitMQ source message id'' AFTER redirect_url'
);

PREPARE add_source_message_id_statement FROM @add_source_message_id;
EXECUTE add_source_message_id_statement;
DEALLOCATE PREPARE add_source_message_id_statement;

-- Preserve all notification rows while retaining only the earliest mapping for a duplicated message id.
UPDATE notification newer_notification
JOIN notification older_notification
  ON older_notification.source_message_id = newer_notification.source_message_id
 AND older_notification.id < newer_notification.id
SET newer_notification.source_message_id = NULL
WHERE newer_notification.source_message_id IS NOT NULL;

SET @add_source_message_unique_index = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'notification'
          AND index_name = 'uk_notification_source_message'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD UNIQUE KEY uk_notification_source_message (source_message_id)'
);

PREPARE add_source_message_unique_index_statement FROM @add_source_message_unique_index;
EXECUTE add_source_message_unique_index_statement;
DEALLOCATE PREPARE add_source_message_unique_index_statement;

SELECT COUNT(*) AS source_message_id_column_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'notification'
  AND column_name = 'source_message_id';

SELECT COUNT(*) AS source_message_unique_index_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'notification'
  AND index_name = 'uk_notification_source_message';
