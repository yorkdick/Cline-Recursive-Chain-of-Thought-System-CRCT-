package com.rayfay.gira.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayfay.gira.dto.request.CreateTaskRequest;
import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.request.UpdateTaskStatusRequest;
import com.rayfay.gira.entity.TaskPriority;
import com.rayfay.gira.entity.TaskStatus;
import com.rayfay.gira.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = { "/sql/task/init_task_test.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = { "/sql/task/cleanup_task_test.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class TaskControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        private String adminToken;
        private String developerToken;
        private Long sprintId;
        private Long taskId;
        private Long userId;

        @BeforeAll
        void setUp() throws Exception {
                // 获取管理员token
                LoginRequest adminLogin = new LoginRequest();
                adminLogin.setUsername("manager");
                adminLogin.setPassword("password");
                MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(adminLogin)))
                                .andReturn();
                adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                                .get("accessToken").asText();

                // 获取开发者token
                LoginRequest devLogin = new LoginRequest();
                devLogin.setUsername("developer");
                devLogin.setPassword("password");
                MvcResult devResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(devLogin)))
                                .andReturn();
                developerToken = objectMapper.readTree(devResult.getResponse().getContentAsString())
                                .get("accessToken").asText();

                // 获取Sprint ID并启动
                MvcResult sprintResult = mockMvc.perform(get("/api/sprints")
                                .header("Authorization", "Bearer " + adminToken))
                                .andReturn();
                sprintId = objectMapper.readTree(sprintResult.getResponse().getContentAsString())
                                .get("content").get(0).get("id").asLong();

                // 启动Sprint
                mockMvc.perform(put("/api/sprints/{id}/start", sprintId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());

                // 获取开发者ID
                userId = userRepository.findByUsername("developer")
                                .orElseThrow(() -> new RuntimeException("测试用户未找到")).getId();
        }

        @Test
        @Order(1)
        void createTask_WithValidRequest_ShouldReturnCreatedTask() throws Exception {
                CreateTaskRequest request = new CreateTaskRequest();
                request.setTitle("Test Task");
                request.setDescription("Test Description");
                request.setPriority(TaskPriority.MEDIUM);
                request.setSprintId(sprintId);

                MvcResult result = mockMvc.perform(post("/api/tasks")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Test Task"))
                                .andExpect(jsonPath("$.description").value("Test Description"))
                                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                                .andExpect(jsonPath("$.status").value("TODO"))
                                .andReturn();

                String response = result.getResponse().getContentAsString();
                taskId = objectMapper.readTree(response).get("id").asLong();
        }

        @Test
        @Order(2)
        void createTask_WithoutTitle_ShouldReturnBadRequest() throws Exception {
                CreateTaskRequest request = new CreateTaskRequest();
                request.setDescription("Test Description");
                request.setPriority(TaskPriority.MEDIUM);
                request.setSprintId(sprintId);

                mockMvc.perform(post("/api/tasks")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("任务标题不能为空"));
        }

        @Test
        @Order(3)
        void createTask_WithInvalidSprint_ShouldReturnBadRequest() throws Exception {
                CreateTaskRequest request = new CreateTaskRequest();
                request.setTitle("Test Task");
                request.setDescription("Test Description");
                request.setPriority(TaskPriority.MEDIUM);
                request.setSprintId(999L);

                mockMvc.perform(post("/api/tasks")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Sprint不存在"));
        }

        @Test
        @Order(4)
        void createTask_WithAssignee_ShouldReturnCreatedTask() throws Exception {
                CreateTaskRequest request = new CreateTaskRequest();
                request.setTitle("Assigned Task");
                request.setDescription("Task with Assignee");
                request.setPriority(TaskPriority.HIGH);
                request.setSprintId(sprintId);
                request.setAssigneeId(userId);

                mockMvc.perform(post("/api/tasks")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Assigned Task"))
                                .andExpect(jsonPath("$.assignee.id").value(userId));
        }

        @Test
        @Order(5)
        void getTask_WithValidId_ShouldReturnTask() throws Exception {
                mockMvc.perform(get("/api/tasks/{id}", taskId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(taskId))
                                .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        @Order(6)
        void getTask_WithInvalidId_ShouldReturnNotFound() throws Exception {
                mockMvc.perform(get("/api/tasks/{id}", 999L)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("任务不存在"));
        }

        @Test
        @Order(7)
        void getAllTasksBySprint_ShouldReturnPageOfTasks() throws Exception {
                mockMvc.perform(get("/api/sprints/{id}/tasks", sprintId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].title").value("测试任务"))
                                .andExpect(jsonPath("$[0].status").value("TODO"))
                                .andExpect(jsonPath("$[0].priority").value("MEDIUM"));
        }

        @Test
        @Order(8)
        void getAllTasksByAssignee_ShouldReturnPageOfTasks() throws Exception {
                mockMvc.perform(get("/api/tasks/assignee/{assigneeId}", userId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].assignee.id").value(userId));
        }

        @Test
        @Order(9)
        void getAllTasksByStatus_ShouldReturnPageOfTasks() throws Exception {
                mockMvc.perform(get("/api/tasks/assignee/{assigneeId}", userId)
                                .param("status", "TODO")
                                .param("priority", "MEDIUM")
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].status").value("TODO"));
        }

        @Test
        @Order(10)
        void updateTask_ToInProgress_ShouldReturnUpdatedTask() throws Exception {
                UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
                request.setStatus(TaskStatus.IN_PROGRESS);

                mockMvc.perform(put("/api/tasks/{id}/status", taskId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @Order(11)
        void updateTask_ToDone_ShouldReturnUpdatedTask() throws Exception {
                UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
                request.setStatus(TaskStatus.DONE);

                mockMvc.perform(put("/api/tasks/{id}/status", taskId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        @Order(12)
        void updateTask_InvalidStatusTransition_ShouldReturnBadRequest() throws Exception {
                // 创建新任务用于测试
                CreateTaskRequest createRequest = new CreateTaskRequest();
                createRequest.setTitle("Status Test Task");
                createRequest.setDescription("For testing invalid status transition");
                createRequest.setPriority(TaskPriority.MEDIUM);
                createRequest.setSprintId(sprintId);

                MvcResult result = mockMvc.perform(post("/api/tasks")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                Long newTaskId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

                // 尝试从TODO直接到DONE
                UpdateTaskStatusRequest updateRequest = new UpdateTaskStatusRequest();
                updateRequest.setStatus(TaskStatus.DONE);

                mockMvc.perform(put("/api/tasks/{id}/status", newTaskId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("无效的状态变更"));
        }

        @Test
        @Order(13)
        void deleteTask_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(delete("/api/tasks/{id}", taskId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(14)
        void deleteTask_WithAdminRole_ShouldReturnOk() throws Exception {
                mockMvc.perform(delete("/api/tasks/{id}", taskId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
        }
}