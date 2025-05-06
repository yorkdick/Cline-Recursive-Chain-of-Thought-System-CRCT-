package com.rayfay.gira.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayfay.gira.dto.request.CreateSprintRequest;
import com.rayfay.gira.dto.request.CreateTaskRequest;
import com.rayfay.gira.dto.request.UpdateSprintRequest;
import com.rayfay.gira.entity.TaskPriority;
import com.rayfay.gira.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.hamcrest.Matchers.*;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = { "/sql/sprint/init_sprint_test.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = { "/sql/sprint/cleanup_sprint_test.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Long developerId;
    private Long planningSprintId;
    private Long secondSprintId;
    private Long activeSprintId;

    @BeforeAll
    void setUp() {
        // 获取测试用户ID
        developerId = userRepository.findByUsername("developer")
                .orElseThrow(() -> new RuntimeException("开发者用户未找到"))
                .getId();
    }

    // 工具方法：创建Sprint
    private Long createTestSprint(String name, LocalDate startDate, LocalDate endDate) throws Exception {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName(name);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        MvcResult result = mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // 工具方法：添加任务到Sprint
    private void addTaskToSprint(Long sprintId, String title) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        request.setDescription("Test task description");
        request.setSprintId(sprintId);
        request.setPriority(TaskPriority.MEDIUM);
        request.setAssigneeId(developerId);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(1)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void createSprint_WithValidRequest_ShouldReturnCreatedSprint() throws Exception {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Planning Sprint");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(2));

        MvcResult result = mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Planning Sprint"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andReturn();

        secondSprintId = createTestSprint("Second Sprint", LocalDate.now(), LocalDate.now().plusWeeks(2));

        // 保存创建的Sprint ID供后续测试使用
        String response = result.getResponse().getContentAsString();
        planningSprintId = objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    @Order(2)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void createSprint_WithDurationMoreThan4Weeks_ShouldReturnBadRequest() throws Exception {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Long Sprint");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(5));

        mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sprint持续时间不能超过4周"));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void createSprint_WithDuplicateName_ShouldReturnBadRequest() throws Exception {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Planning Sprint"); // 使用已存在的Sprint名称
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(2));

        mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sprint名称已存在"));
    }

    @Test
    @Order(4)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void startSprint_WithNoTasks_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/sprints/{id}/start", planningSprintId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sprint中没有任务，无法启动"));
    }

    @Test
    @Order(5)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void startSprint_WithTasks_ShouldCreateBoard() throws Exception {
        // 添加任务到已存在的Planning Sprint
        addTaskToSprint(planningSprintId, "Task for Board Test");

        // 启动Sprint
        mockMvc.perform(put("/api/sprints/{id}/start", planningSprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.board").exists())
                .andExpect(jsonPath("$.board.status").value("ACTIVE"));

        // 保存活动Sprint ID供后续测试使用
        activeSprintId = planningSprintId;
    }

    @Test
    @Order(6)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void startSprint_WhenAnotherSprintActive_ShouldReturnBadRequest() throws Exception {
        // 验证已有活动Sprint时，不能启动新Sprint
        mockMvc.perform(put("/api/sprints/{id}/start", secondSprintId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已存在活动中的Sprint"));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void completeSprint_ShouldCompleteAllTasksAndArchiveBoard() throws Exception {
        // 使用已经处于活动状态的Sprint
        mockMvc.perform(put("/api/sprints/{id}/complete", activeSprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.board.status").value("ARCHIVED"));

        // 验证所有任务都被完成
        mockMvc.perform(get("/api/sprints/{id}/tasks", activeSprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("DONE"))));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void completeSprint_WhenNotActive_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/sprints/{id}/complete", secondSprintId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能完成活动中的Sprint"));
    }

    @Test
    @Order(9)
    @WithMockUser(username = "developer", roles = "DEVELOPER")
    void updateSprint_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        UpdateSprintRequest request = new UpdateSprintRequest();
        request.setName("Updated Sprint");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(2));

        mockMvc.perform(put("/api/sprints/{id}", secondSprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @WithMockUser(username = "manager", roles = { "ADMIN" })
    void updateSprint_WhenNotInPlanning_ShouldReturnBadRequest() throws Exception {
        addTaskToSprint(secondSprintId, "Task for Update Test");
        mockMvc.perform(put("/api/sprints/{id}/start", secondSprintId))
                .andExpect(status().isOk());

        UpdateSprintRequest request = new UpdateSprintRequest();
        request.setName("Updated Sprint");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusWeeks(2));

        mockMvc.perform(put("/api/sprints/{id}", secondSprintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能修改计划中的Sprint"));
    }

    @Test
    @Order(11)
    @WithMockUser(username = "developer", roles = "DEVELOPER")
    void getSprint_WithValidId_ShouldReturnSprint() throws Exception {
        // 使用已完成的Sprint进行测试
        mockMvc.perform(get("/api/sprints/{id}", activeSprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeSprintId))
                .andExpect(jsonPath("$.name").value("Planning Sprint"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @Order(12)
    @WithMockUser(username = "developer", roles = "DEVELOPER")
    void getSprint_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/sprints/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sprint不存在"));
    }
}