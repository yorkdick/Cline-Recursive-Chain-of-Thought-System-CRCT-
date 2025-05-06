-- H2数据库初始化脚本
-- 插入管理员用户
-- 密码是: admin123
INSERT INTO users (username, password, email, full_name, role, status, created_at, updated_at)
VALUES ('admin', '$2a$10$DsTMvqAkDuep4jMoQE.kce3N8vJDnc6sHYd9.BxhJ0tL178gFfv1K', 'admin@example.com', '系统管理员', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
