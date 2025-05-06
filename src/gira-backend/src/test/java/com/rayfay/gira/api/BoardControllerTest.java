package com.rayfay.gira.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.request.UpdateBoardRequest;
import com.rayfay.gira.entity.BoardStatus;
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
@Sql(scripts = { "/sql/board/init_board_test.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = { "/sql/board/cleanup_board_test.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class BoardControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String adminToken;
        private String developerToken;
        private Long boardId;

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
                System.out.println(sprintResult.getResponse().getContentAsString());
                Long sprintId = objectMapper.readTree(sprintResult.getResponse().getContentAsString())
                                .get("content").get(0).get("id").asLong();

                // 启动Sprint
                mockMvc.perform(put("/api/sprints/{id}/start", sprintId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());

                // 获取Sprint对应的看板ID
                MvcResult result = mockMvc.perform(get("/api/boards/active")
                                .header("Authorization", "Bearer " + adminToken))
                                .andReturn();
                boardId = objectMapper.readTree(result.getResponse().getContentAsString())
                                .get("id").asLong();
        }

        @Test
        @Order(1)
        void updateBoard_AsAdmin_ShouldSucceed() throws Exception {
                UpdateBoardRequest request = new UpdateBoardRequest();
                request.setName("更新后的看板");
                request.setDescription("测试更新看板");

                mockMvc.perform(put("/api/boards/{id}", boardId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("更新后的看板"))
                                .andExpect(jsonPath("$.description").value("测试更新看板"));
        }

        @Test
        @Order(2)
        void updateBoard_AsDeveloper_ShouldReturnForbidden() throws Exception {
                UpdateBoardRequest request = new UpdateBoardRequest();
                request.setName("开发者修改");
                request.setDescription("测试开发者修改看板");

                mockMvc.perform(put("/api/boards/{id}", boardId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(3)
        void getBoard_AsAdmin_ShouldSucceed() throws Exception {
                mockMvc.perform(get("/api/boards/{id}", boardId)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(boardId));
        }

        @Test
        @Order(4)
        void getBoard_AsDeveloper_ShouldSucceed() throws Exception {
                mockMvc.perform(get("/api/boards/{id}", boardId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(boardId));
        }

        @Test
        @Order(5)
        void getActiveBoard_ShouldReturnSprintBoard() throws Exception {
                mockMvc.perform(get("/api/boards/active")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(boardId))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @Order(6)
        void getBoards_WithStatusFilter_ShouldReturnFilteredBoards() throws Exception {
                mockMvc.perform(get("/api/boards")
                                .param("status", BoardStatus.ACTIVE.name())
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }
}