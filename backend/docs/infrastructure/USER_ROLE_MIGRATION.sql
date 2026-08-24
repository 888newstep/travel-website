-- 为现有用户表增加角色字段。默认值为普通用户，不自动授予管理员权限。
-- 脚本可重复执行，适用于 MySQL 5.7/8.x。

SET @schema_name = DATABASE();

SET @add_user_type = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'user'
          AND column_name = 'user_type'
    ),
    'SELECT 1',
    'ALTER TABLE `user` ADD COLUMN user_type INT NOT NULL DEFAULT 1 COMMENT ''用户类型(1:普通用户,9:管理员)'' AFTER phone'
);
PREPARE add_user_type_statement FROM @add_user_type;
EXECUTE add_user_type_statement;
DEALLOCATE PREPARE add_user_type_statement;

-- 由数据库管理员明确指定管理员账号，例如：
-- UPDATE `user` SET user_type = 9 WHERE id = 1;
