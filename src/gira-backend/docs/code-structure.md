# GIRA 后端代码结构文档

## 1. 整体架构
项目采用典型的分层架构，主要包含以下层次：
- 表现层（Controller）
- 业务层（Service）
- 数据访问层（Repository）
- 安全层（Security）

### 1.1 核心包结构
```
com.rayfay.gira/
├── config/          # 配置类
├── controller/      # 控制器
├── service/         # 服务层
├── repository/      # 数据访问层
├── entity/          # 实体类
├── dto/             # 数据传输对象
├── mapper/          # 对象映射
├── security/        # 安全相关
└── exception/       # 异常处理
```

## 2. 关键模块详解

### 2.1 配置模块 (config/)
- `SecurityConfig.java`: Spring Security 主配置类，负责安全规则配置
- `AuthenticationConfig.java`: 认证相关配置
- `SecurityBeanConfig.java`: 安全相关 Bean 的配置

### 2.2 实体模块 (entity/)
核心实体类：
- `User.java`: 用户实体
- `Board.java`: 看板实体
- `Sprint.java`: 迭代实体
- `Task.java`: 任务实体
- `BoardColumn.java`: 看板列实体

状态枚举：
- `UserStatus.java`: 用户状态
- `BoardStatus.java`: 看板状态
- `SprintStatus.java`: 迭代状态
- `TaskStatus.java`: 任务状态
- `TaskPriority.java`: 任务优先级
- `UserRole.java`: 用户角色

### 2.3 控制器模块 (controller/)
- `AuthController.java`: 认证相关接口
- `UserController.java`: 用户管理接口
- `BoardController.java`: 看板管理接口
- `SprintController.java`: 迭代管理接口
- `TaskController.java`: 任务管理接口

### 2.4 服务层模块 (service/)
分为接口定义和实现两部分：
- `interfaces/`: 服务接口定义
- `impl/`: 服务实现类

### 2.5 安全模块 (security/)
- `JwtTokenProvider.java`: JWT 令牌生成和验证
- `JwtAuthenticationFilter.java`: JWT 认证过滤器

## 3. 关键流程

### 3.1 认证流程
1. 客户端发送登录请求到 `/api/auth/login`
2. `AuthController` 处理登录请求
3. `JwtTokenProvider` 生成 JWT 令牌
4. 后续请求通过 `JwtAuthenticationFilter` 进行认证

### 3.2 业务流程
1. 控制器接收请求并进行参数验证
2. 调用相应的服务层处理业务逻辑
3. 服务层通过 Repository 访问数据库
4. 使用 Mapper 进行 DTO 和实体间的转换
5. 返回处理结果

## 4. 开发指南

### 4.1 添加新功能
1. 在 entity/ 添加新的实体类
2. 在 repository/ 添加相应的数据访问接口
3. 在 dto/ 添加数据传输对象
4. 在 mapper/ 添加对象映射器
5. 在 service/ 添加业务逻辑
6. 在 controller/ 添加 API 接口

### 4.2 问题定位
1. 接口问题：查看对应的 Controller
2. 业务逻辑问题：查看对应的 Service 实现
3. 数据访问问题：查看对应的 Repository
4. 认证问题：查看 security/ 和 config/ 下的实现

## 5. 测试结构
```
test/
├── java/
│   └── com/rayfay/gira/
│       ├── api/        # API 测试
│       └── service/    # 服务层测试
└── resources/          # 测试资源 