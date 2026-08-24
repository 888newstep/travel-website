-- 用户手机号唯一约束。NULL 仍允许多个，兼容历史未绑定手机号的账号。
SET @user_phone_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'uk_user_phone'
);

SET @user_phone_index_sql = IF(
    @user_phone_index_exists = 0,
    'ALTER TABLE user ADD UNIQUE KEY uk_user_phone (phone)',
    'SELECT 1'
);
PREPARE user_phone_index_stmt FROM @user_phone_index_sql;
EXECUTE user_phone_index_stmt;
DEALLOCATE PREPARE user_phone_index_stmt;
