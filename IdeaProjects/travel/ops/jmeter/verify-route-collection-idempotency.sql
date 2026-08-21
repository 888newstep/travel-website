-- JMeter 执行完成后运行。本脚本不会修改数据，可重复执行。
-- 可在 SOURCE 前覆盖变量：SET @test_username='zhangsan'; SET @test_route_id=1;

SET @test_username = COALESCE(@test_username, 'zhangsan');
SET @test_route_id = COALESCE(@test_route_id, 1);
SET @test_user_id = (
    SELECT id
    FROM `user`
    WHERE username = @test_username OR phone = @test_username
    ORDER BY CASE WHEN username = @test_username THEN 0 ELSE 1 END
    LIMIT 1
);

SELECT
    @test_username AS test_username,
    @test_user_id AS test_user_id,
    @test_route_id AS test_route_id;

SELECT
    COUNT(*) AS actual_business_rows,
    CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS idempotency_result
FROM user_collection
WHERE user_id = @test_user_id
  AND item_id = @test_route_id
  AND item_type = 'route'
  AND collection_type = 'collect';

SELECT
    id,
    user_id,
    item_id,
    item_type,
    collection_type,
    created_at
FROM user_collection
WHERE user_id = @test_user_id
  AND item_id = @test_route_id
  AND item_type = 'route'
  AND collection_type = 'collect'
ORDER BY id;

SELECT
    'uk_user_item_action' AS expected_index,
    COUNT(*) AS indexed_column_count,
    MIN(non_unique) AS non_unique,
    GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS indexed_columns,
    CASE
        WHEN COUNT(*) = 4
         AND MIN(non_unique) = 0
         AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') =
             'user_id,item_id,item_type,collection_type'
        THEN 'PASS'
        ELSE 'FAIL'
    END AS unique_index_result
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'user_collection'
  AND index_name = 'uk_user_item_action';
