-- RabbitMQ message status table migration for an existing travel_website database.
-- Execute manually after backup; this script is intentionally not auto-imported by Spring Boot.

CREATE DATABASE IF NOT EXISTS travel_website
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE travel_website;

CREATE TABLE IF NOT EXISTS `mq_message_status` (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'message status record id',
    message_id         VARCHAR(64) NOT NULL COMMENT 'RabbitMQ correlation id',
    message_type       VARCHAR(64) NOT NULL COMMENT 'message payload type',
    exchange_name      VARCHAR(255) NOT NULL COMMENT 'exchange name',
    routing_key        VARCHAR(255) NOT NULL COMMENT 'routing key',
    payload_json       LONGTEXT NOT NULL COMMENT 'serialized payload for later compensation',
    status             VARCHAR(20) NOT NULL COMMENT 'PENDING/DISPATCHED/CONFIRMED/RETURNED/FAILED',
    retry_count        INT NOT NULL DEFAULT 0 COMMENT 'compensation retry count',
    last_error         VARCHAR(1000) COMMENT 'latest publish error',
    next_attempt_time  TIMESTAMP NULL COMMENT 'next compensation time',
    dispatched_at      TIMESTAMP NULL COMMENT 'local RabbitTemplate return time',
    confirmed_at       TIMESTAMP NULL COMMENT 'broker publisher confirm time',
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'creation time',
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_mq_message_id (message_id),
    INDEX idx_mq_status_next_attempt (status, next_attempt_time),
    INDEX idx_mq_created_at (created_at)
) COMMENT='RabbitMQ message publish status' ENGINE=InnoDB;

-- Verify the migration before enabling MQ_STATUS_PERSISTENCE_ENABLED.
SELECT COUNT(*) AS mq_message_status_table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'mq_message_status';

SHOW CREATE TABLE `mq_message_status`;
