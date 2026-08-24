-- 当前项目尚未建设游记评论明细模型，旧初始化数据中的评论数没有可验证来源。
UPDATE travel_note
SET comments_count = 0
WHERE comments_count <> 0;
