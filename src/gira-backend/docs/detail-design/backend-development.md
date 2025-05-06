# GIRA后台开发指导文档

## 1. 系统架构实现

### 1.1 项目结构
```
src/main/java/com/rayfay/gira/
├── config/           # 配置类
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── WebConfig.java
├── controller/       # 控制器
│   ├── AuthController.java
│   ├── UserController.java
│   ├── BoardController.java
│   ├── SprintController.java
│   └── TaskController.java
├── service/         # 业务层
│   ├── interfaces/  # 服务接口
│   └── impl/        # 服务实现
├── repository/      # 数据访问层
├── entity/          # 实体类
├── dto/             # 数据传输对象
│   ├── request/     # 请求对象
│   └── response/    # 响应对象
├── mapper/          # 对象映射
├── security/        # 安全相关
└── common/          # 公共组件
    ├── exception/   # 异常处理
    └── utils/       # 工具类
```

### 1.2 配置实现

#### 1.2.1 安全配置
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

#### 1.2.2 统一响应处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}

@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public Object beforeBodyWrite(Object body, ...) {
        if (body instanceof ErrorResponse) {
            return body;
        }
        return new ApiResponse<>(200, "success", body);
    }
}
```

## 2. 核心功能实现

### 2.1 用户认证实现
```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 验证用户名密码
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
            
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("用户名或密码错误");
        }
        
        // 生成Token
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        
        return new LoginResponse(accessToken, refreshToken, tokenProvider.getAccessTokenValidityInSeconds());
    }
    
    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("无效的刷新令牌");
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationException("用户不存在"));
            
        String accessToken = tokenProvider.generateAccessToken(user);
        
        return new TokenResponse(accessToken, tokenProvider.getAccessTokenValidityInSeconds());
    }
}
```

### 2.2 Sprint管理实现
```java
@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {
    
    private final SprintRepository sprintRepository;
    private final BoardService boardService;
    private final TaskRepository taskRepository;
    private final SprintMapper sprintMapper;
    
    @Override
    @Transactional
    public SprintResponse startSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));
            
        // 检查是否有其他进行中的Sprint
        if (sprintRepository.existsByStatus(SprintStatus.ACTIVE)) {
            throw new BusinessException("已存在活动中的Sprint");
        }
        
        // 检查Sprint状态
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new BusinessException("只能启动计划中的Sprint");
        }
        
        // 检查是否有任务
        if (taskRepository.countBySprintId(sprint.getId()) == 0) {
            throw new BusinessException("Sprint中没有任务，无法启动");
        }
        
        // 创建看板
        Board board = Board.builder()
            .name(sprint.getName() + "看板")
            .status(BoardStatus.ACTIVE)
            .createdBy(sprint.getCreatedBy())
            .build();
        boardService.createBoard(board);
        
        // 更新Sprint状态
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setBoard(board);
        
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }
    
    @Override
    @Transactional
    public SprintResponse completeSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));
            
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new BusinessException("只能完成活动中的Sprint");
        }
        
        // 完成所有未完成的任务
        List<Task> unfinishedTasks = taskRepository.findBySprintIdAndStatusIn(
            sprint.getId(),
            List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)
        );
        
        unfinishedTasks.forEach(task -> {
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
        });
        
        // 归档看板
        Board board = sprint.getBoard();
        if (board != null) {
            board.setStatus(BoardStatus.ARCHIVED);
            boardService.updateBoard(board);
        }
        
        // 更新Sprint状态
        sprint.setStatus(SprintStatus.COMPLETED);
        
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }
}
```

### 2.3 任务管理实现
```java
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    
    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
            
        // 检查任务状态
        if (task.getStatus() == TaskStatus.DONE) {
            throw new BusinessException("已完成的任务不能更新状态");
        }
        
        // 验证状态转换
        if (!isValidStatusTransition(task.getStatus(), request.getStatus())) {
            throw new BusinessException("无效的状态变更");
        }
        
        task.setStatus(request.getStatus());
        return taskMapper.toResponse(taskRepository.save(task));
    }
    
    private boolean isValidStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        
        switch (currentStatus) {
            case TODO:
                return newStatus == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS:
                return newStatus == TaskStatus.DONE || newStatus == TaskStatus.TODO;
            case DONE:
                return false;
            default:
                return false;
        }
    }
    
    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Sprint sprint = sprintRepository.findById(request.getSprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));
            
        // 检查Sprint状态
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            throw new BusinessException("不能在已完成的Sprint中创建任务");
        }
        
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .sprint(sprint)
            .reporter(getCurrentUser())
            .priority(request.getPriority())
            .status(TaskStatus.TODO)
            .build();
        
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("指派人不存在"));
            task.setAssignee(assignee);
        }
        
        return taskMapper.toResponse(taskRepository.save(task));
    }
}
```

### 2.4 看板管理实现
```java
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    
    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;
    private final BoardMapper boardMapper;
    
    @Override
    @Transactional
    public BoardResponse updateBoard(Long id, UpdateBoardRequest request) {
        Board board = boardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("看板不存在"));
            
        if (board.getStatus() == BoardStatus.ARCHIVED) {
            throw new BusinessException("已归档的看板不能修改");
        }
        
        // 检查名称唯一性
        if (!board.getName().equals(request.getName()) &&
                boardRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("看板名称已存在");
        }
        
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        
        return boardMapper.toResponse(boardRepository.save(board));
    }
    
    @Override
    public BoardResponse getActiveBoard() {
        Board board = boardRepository.findByStatus(BoardStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("没有活动的看板"));
            
        BoardResponse response = boardMapper.toResponse(board);
        
        // 获取看板任务
        List<Task> tasks = taskRepository.findBySprintId(board.getSprint().getId());
        Map<TaskStatus, List<TaskResponse>> tasksByStatus = tasks.stream()
            .map(taskMapper::toResponse)
            .collect(groupingBy(TaskResponse::getStatus));
            
        response.setTasks(tasksByStatus);
        
        return response;
    }
}
```

## 3. 单元测试示例

### 3.1 Service层测试
```java
@ExtendWith(MockitoExtension.class)
class SprintServiceTest {
    
    @Mock
    private SprintRepository sprintRepository;
    
    @Mock
    private BoardService boardService;
    
    @Mock
    private TaskRepository taskRepository;
    
    @InjectMocks
    private SprintServiceImpl sprintService;
    
    @Test
    void startSprint_ShouldSuccess() {
        // Given
        Long sprintId = 1L;
        Sprint sprint = createTestSprint(sprintId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByStatus(SprintStatus.ACTIVE)).thenReturn(false);
        when(taskRepository.countBySprintId(sprintId)).thenReturn(1);
        
        // When
        SprintResponse response = sprintService.startSprint(sprintId);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(SprintStatus.ACTIVE);
        verify(boardService).createBoard(any(Board.class));
        verify(sprintRepository).save(sprint);
    }
    
    @Test
    void startSprint_WhenActiveSprintExists_ShouldThrowException() {
        // Given
        Long sprintId = 1L;
        Sprint sprint = createTestSprint(sprintId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByStatus(SprintStatus.ACTIVE)).thenReturn(true);
        
        // When & Then
        assertThrows(BusinessException.class, () -> sprintService.startSprint(sprintId));
    }
}
```

### 3.2 Controller层测试
```java
@WebMvcTest(SprintController.class)
class SprintControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private SprintService sprintService;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createSprint_ShouldSuccess() throws Exception {
        // Given
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Sprint 1");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(2));
        
        SprintResponse response = new SprintResponse();
        response.setId(1L);
        response.setName(request.getName());
        
        when(sprintService.createSprint(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("Sprint 1"));
    }
}
```