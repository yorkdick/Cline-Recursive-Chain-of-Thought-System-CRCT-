# 后台接口文档

## 1. 用户管理模块 (UserController)

### 1.1 创建用户
- **权限**: ADMIN
- **方法**: POST
- **路径**: /api/users
- **请求体**:
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string",
    "fullName": "string",
    "role": "ADMIN|DEVELOPER",
    "status": "ACTIVE|INACTIVE"
  }
  ```
- **响应**: UserResponse
- **校验**: 用户名唯一性、邮箱格式、密码复杂度

### 1.2 更新用户
- **方法**: PUT
- **路径**: /api/users/{id}
- **请求体**:
  ```json
  {
    "email": "string",
    "fullName": "string",
    "status": "ACTIVE|INACTIVE",
    "role": "ADMIN|USER"
  }
  ```
- **响应**: UserResponse
- **校验**: 
  - 邮箱格式
  - 角色修改需要管理员权限
  - 不能修改最后一个管理员的角色

### 1.3 修改密码
- **方法**: PUT
- **路径**: /api/users/{id}/password
- **请求体**:
  ```json
  {
    "oldPassword": "string",
    "newPassword": "string"
  }
  ```
- **响应**: 空
- **校验**: 旧密码验证、新密码复杂度

### 1.4 获取用户列表
- **权限**: ADMIN
- **方法**: GET
- **路径**: /api/users
- **参数**: page, size, sort
- **响应**: Page<UserResponse>

### 1.5 获取当前用户
- **方法**: GET
- **路径**: /api/users/current
- **响应**: UserResponse

## 2. 认证模块 (AuthController)

### 2.1 登录
- **方法**: POST
- **路径**: /api/auth/login
- **请求体**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **响应**: LoginResponse (包含accessToken和refreshToken)

### 2.2 登出
- **方法**: POST
- **路径**: /api/auth/logout
- **响应**: 空

### 2.3 刷新Token
- **方法**: POST
- **路径**: /api/auth/refresh-token
- **请求体**:
  ```json
  {
    "refreshToken": "string"
  }
  ```
- **响应**: 新的accessToken或错误信息

## 3. 看板管理模块 (BoardController)

### 3.1 更新看板
- **权限**: ADMIN
- **方法**: PUT
- **路径**: /api/boards/{id}
- **请求体**:
  ```json
  {
    "name": "string",
    "description": "string",
    "status": "ACTIVE|ARCHIVED"
  }
  ```
- **响应**: BoardResponse

### 3.2 获取看板详情
- **方法**: GET
- **路径**: /api/boards/{id}
- **响应**: BoardResponse

### 3.3 获取活跃看板
- **方法**: GET
- **路径**: /api/boards/active
- **响应**: BoardResponse

### 3.4 获取看板任务列表
- **方法**: GET
- **路径**: /api/boards/{id}/tasks
- **响应**: List<TaskResponse>

### 3.5 获取看板列表
- **方法**: GET
- **路径**: /api/boards
- **参数**: 
  - status: ACTIVE|ARCHIVED
  - page: 页码
  - size: 每页数量
  - sort: 排序字段
- **响应**: Page<BoardResponse>

## 4. 迭代管理模块 (SprintController)

### 4.1 创建迭代
- **权限**: ADMIN
- **方法**: POST
- **路径**: /api/sprints
- **请求体**:
  ```json
  {
    "name": "string",
    "description": "string",
    "startDate": "yyyy-MM-dd",
    "endDate": "yyyy-MM-dd"
  }
  ```
- **响应**: SprintResponse

### 4.2 更新迭代
- **权限**: ADMIN
- **方法**: PUT
- **路径**: /api/sprints/{id}
- **请求体**:
  ```json
  {
    "name": "string",
    "description": "string",
    "startDate": "yyyy-MM-dd",
    "endDate": "yyyy-MM-dd"
  }
  ```
- **响应**: SprintResponse

### 4.3 获取迭代详情
- **方法**: GET
- **路径**: /api/sprints/{id}
- **响应**: SprintResponse

### 4.4 开始迭代
- **权限**: ADMIN
- **方法**: PUT
- **路径**: /api/sprints/{id}/start
- **响应**: SprintResponse

### 4.5 完成迭代
- **权限**: ADMIN
- **方法**: PUT
- **路径**: /api/sprints/{id}/complete
- **响应**: SprintResponse

### 4.6 获取迭代列表
- **方法**: GET
- **路径**: /api/sprints
- **参数**:
  - status: PLANNING|IN_PROGRESS|COMPLETED
  - page: 页码
  - size: 每页数量
  - sort: 排序字段
- **响应**: Page<SprintResponse>

### 4.7 获取迭代任务列表
- **方法**: GET
- **路径**: /api/sprints/{id}/tasks
- **响应**: List<TaskResponse>

## 5. 任务管理模块 (TaskController)

### 5.1 创建任务
- **方法**: POST
- **路径**: /api/tasks
- **请求体**:
  ```json
  {
    "title": "string",
    "description": "string",
    "priority": "LOW|MEDIUM|HIGH",
    "status": "TODO|IN_PROGRESS|DONE",
    "boardId": 0,
    "assigneeId": 0
  }
  ```
- **响应**: TaskResponse

### 5.2 更新任务
- **方法**: PUT
- **路径**: /api/tasks/{id}
- **请求体**:
  ```json
  {
    "title": "string",
    "description": "string",
    "priority": "LOW|MEDIUM|HIGH"
  }
  ```
- **响应**: TaskResponse

### 5.3 更新任务状态
- **方法**: PUT
- **路径**: /api/tasks/{id}/status
- **请求体**:
  ```json
  {
    "status": "TODO|IN_PROGRESS|DONE"
  }
  ```
- **响应**: TaskResponse

### 5.4 获取任务详情
- **方法**: GET
- **路径**: /api/tasks/{id}
- **响应**: TaskResponse

### 5.5 移动任务到迭代
- **方法**: PUT
- **路径**: /api/tasks/{id}/sprint/{sprintId}
- **响应**: TaskResponse

### 5.6 获取用户任务列表
- **方法**: GET
- **路径**: /api/tasks/assignee/{assigneeId}
- **参数**:
  - page: 页码
  - size: 每页数量
- **响应**: Page<TaskResponse>

### 5.7 删除任务
- **权限**: ADMIN
- **方法**: DELETE
- **路径**: /api/tasks/{id}
- **响应**: 空
