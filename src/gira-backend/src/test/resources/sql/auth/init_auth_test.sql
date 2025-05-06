-- 清理测试用户数据
DELETE FROM users WHERE username IN ('manager', 'developer');

-- 插入测试用户
INSERT INTO users (username, password, email, full_name, role, status, created_at, updated_at)
VALUES 
('manager', '$2a$10$IHTHaQOBDsQP4NotUG1QY.C9EXg5sDkEUOIo/29bs2a2HGe.9J3J2', 'manager@example.com', '项目经理', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('developer', '$2a$10$IHTHaQOBDsQP4NotUG1QY.C9EXg5sDkEUOIo/29bs2a2HGe.9J3J2', 'developer@example.com', '开发人员', 'DEVELOPER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 