-- 插入测试用户
INSERT INTO users (username, password, email, full_name, role, status, created_at, updated_at)
VALUES 
    ('manager', '$2a$10$IHTHaQOBDsQP4NotUG1QY.C9EXg5sDkEUOIo/29bs2a2HGe.9J3J2', 'manager@example.com', 'Board Manager', 'ADMIN', 'ACTIVE', NOW(), NOW()),
    ('developer', '$2a$10$IHTHaQOBDsQP4NotUG1QY.C9EXg5sDkEUOIo/29bs2a2HGe.9J3J2', 'dev@example.com', 'Board Developer', 'DEVELOPER', 'ACTIVE', NOW(), NOW());

-- 插入测试Sprint
INSERT INTO sprints (name, start_date, end_date, status, created_by, created_at, updated_at)
SELECT 
    '测试Sprint',
    CURRENT_DATE,
    DATEADD('WEEK', 2, CURRENT_DATE),
    'PLANNING',
    id,
    NOW(),
    NOW()
FROM users 
WHERE username = 'manager';

-- 插入测试任务
INSERT INTO tasks (title, description, priority, status, sprint_id, assignee_id, reporter_id, created_at, updated_at)
SELECT 
    '测试任务',
    '这是一个测试任务',
    'MEDIUM',
    'TODO',
    s.id,
    u2.id,
    u1.id,
    NOW(),
    NOW()
FROM sprints s
CROSS JOIN users u1
CROSS JOIN users u2
WHERE u1.username = 'manager'
AND u2.username = 'developer'; 