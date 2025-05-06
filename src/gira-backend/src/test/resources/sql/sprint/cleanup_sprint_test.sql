-- 清理测试数据，按照外键依赖关系的顺序清理
DELETE FROM tasks WHERE assignee_id IN (SELECT id FROM users WHERE username IN ('manager', 'developer'))
   OR reporter_id IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));
DELETE FROM boards WHERE created_by IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));
DELETE FROM sprints WHERE created_by IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));
DELETE FROM users WHERE username IN ('manager', 'developer'); 