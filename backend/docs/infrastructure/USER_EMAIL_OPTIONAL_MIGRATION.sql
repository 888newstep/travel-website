-- 手机号是当前注册流程的必填身份标识，邮箱允许后续在个人资料中补充。
-- 本迁移可重复执行；已有唯一索引保持不变，MySQL 允许唯一索引中存在多个 NULL。
SET @schema_name = DATABASE();

SET @make_email_nullable = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'user'
          AND column_name = 'email'
          AND is_nullable = 'NO'
    ),
    'ALTER TABLE `user` MODIFY COLUMN email VARCHAR(100) NULL COMMENT ''邮箱（手机号注册时可为空）''',
    'SELECT 1'
);

PREPARE make_email_nullable_statement FROM @make_email_nullable;
EXECUTE make_email_nullable_statement;
DEALLOCATE PREPARE make_email_nullable_statement;
