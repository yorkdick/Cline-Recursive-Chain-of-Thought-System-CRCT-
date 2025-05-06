-- 清理测试数据
DELETE FROM boards WHERE created_by IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));
DELETE FROM sprints WHERE created_by IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));
DELETE FROM users WHERE username IN ('manager', 'developer');