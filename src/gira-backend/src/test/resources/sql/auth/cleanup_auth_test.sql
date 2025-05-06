-- 清理测试用户数据
DELETE FROM users WHERE username IN ('manager', 'developer', 'newuser', 'newadmin', 'anotheruser', 'adminuser'); 