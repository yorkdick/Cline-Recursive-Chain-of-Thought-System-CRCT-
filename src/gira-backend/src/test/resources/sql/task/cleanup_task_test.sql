-- 清理测试数据
-- 1. 删除测试任务
DELETE FROM tasks 
WHERE reporter_id IN (SELECT id FROM users WHERE username IN ('manager', 'developer'))
   OR assignee_id IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));

-- 2. 删除测试Sprint
DELETE FROM sprints WHERE created_by IN (SELECT id FROM users WHERE username IN ('manager', 'developer'));

-- 3. 删除测试用户
DELETE FROM users WHERE username IN ('manager', 'developer'); 