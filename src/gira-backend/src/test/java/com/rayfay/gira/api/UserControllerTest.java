package com.rayfay.gira.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayfay.gira.dto.request.CreateUserRequest;
import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.request.UpdatePasswordRequest;
import com.rayfay.gira.dto.request.UpdateUserRequest;
import com.rayfay.gira.entity.UserRole;
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
@Sql(scripts = { "/sql/auth/init_auth_test.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = { "/sql/auth/cleanup_auth_test.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private String superAdmin;
        private String adminToken;
        private String developerToken;
        private String newUserToken;
        private Long newUserId;
        private Long newAdminId;
        private Long adminId;
        private Long superAdminId;

        @BeforeAll
        void setUp() throws Exception {
                // SuperAdmin
                LoginRequest superLogin = new LoginRequest();
                superLogin.setUsername("admin");
                superLogin.setPassword("admin123");
                MvcResult superResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(superLogin)))
                                .andReturn();
                superAdmin = objectMapper.readTree(superResult.getResponse().getContentAsString())
                                .get("accessToken").asText();

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
        }

        @Test
        @Order(1)
        void createUser_AsAdmin_ShouldSucceed() throws Exception {
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername("newuser");
                request.setPassword("password");
                request.setEmail("newuser@example.com");
                request.setFullName("New User");
                request.setRole(UserRole.DEVELOPER);

                MvcResult result = mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("newuser"))
                                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                                .andReturn();

                // 保存新创建的用户ID供后续测试使用
                newUserId = objectMapper.readTree(result.getResponse().getContentAsString())
                                .get("id").asLong();
        }

        @Test
        @Order(2)
        void newUserLogin_ShouldSucceed() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("newuser");
                loginRequest.setPassword("password");
                MvcResult newUserResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andReturn();
                newUserToken = objectMapper.readTree(newUserResult.getResponse().getContentAsString())
                                .get("accessToken").asText();
        }

        @Test
        @Order(3)
        void createUser_AsDeveloper_ShouldReturnForbidden() throws Exception {
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername("anotheruser");
                request.setPassword("password");
                request.setEmail("another@example.com");
                request.setFullName("Another User");
                request.setRole(UserRole.DEVELOPER);

                mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(4)
        void createUser_WithDuplicateUsername_ShouldReturnBadRequest() throws Exception {
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername("newuser"); // 使用已存在的用户名
                request.setPassword("password");
                request.setEmail("different@example.com");
                request.setFullName("Different User");
                request.setRole(UserRole.DEVELOPER);

                mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("用户名已存在"));
        }

        @Test
        @Order(5)
        void updateUser_AsAdmin_ShouldSucceed() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setEmail("updated@example.com");
                request.setFullName("Updated User");

                mockMvc.perform(put("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.fullName").value("Updated User"));
        }

        @Test
        @Order(6)
        void updateUser_AsDeveloper_ShouldReturnForbidden() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setEmail("hacked@example.com");
                request.setFullName("Hacked User");

                mockMvc.perform(put("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(7)
        void updatePassword_ForSelf_ShouldSucceed() throws Exception {
                UpdatePasswordRequest request = new UpdatePasswordRequest();
                request.setOldPassword("password");
                request.setNewPassword("newpassword");

                // 获取当前用户ID
                MvcResult userResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + developerToken))
                                .andReturn();
                Long userId = objectMapper.readTree(userResult.getResponse().getContentAsString())
                                .get("id").asLong();

                mockMvc.perform(put("/api/users/{id}/password", userId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // 验证使用新密码可以登录
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("developer");
                loginRequest.setPassword("newpassword");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk());
        }

        @Test
        @Order(8)
        void updatePassword_WithWrongOldPassword_ShouldReturnBadRequest() throws Exception {
                UpdatePasswordRequest request = new UpdatePasswordRequest();
                request.setOldPassword("wrongpassword");
                request.setNewPassword("newpassword");

                // 获取当前用户ID
                MvcResult userResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + developerToken))
                                .andReturn();
                Long userId = objectMapper.readTree(userResult.getResponse().getContentAsString())
                                .get("id").asLong();

                mockMvc.perform(put("/api/users/{id}/password", userId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("原密码错误"));
        }

        @Test
        @Order(9)
        void getCurrentUser_ShouldReturnUserInfo() throws Exception {
                mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("developer"))
                                .andExpect(jsonPath("$.role").value("DEVELOPER"));
        }

        @Test
        @Order(10)
        void getUsers_AsAdmin_ShouldReturnAllUsers() throws Exception {
                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @Order(11)
        void getUsers_AsDeveloper_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(12) // 修改管理员角色
        void updateAdminRole_ShouldSucceed() throws Exception {
                // 管理员ID
                MvcResult adminResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + adminToken))
                                .andReturn();
                adminId = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                                .get("id").asLong();

                UpdateUserRequest request = new UpdateUserRequest();
                request.setRole(UserRole.DEVELOPER);

                mockMvc.perform(put("/api/users/{id}", adminId)
                                .header("Authorization", "Bearer " + superAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @Order(13) // 修改管理员角色, 只剩最后一个管理员
        void updateAdminRole_ShouldFailed() throws Exception {
                // 管理员ID
                MvcResult adminResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + superAdmin))
                                .andReturn();
                superAdminId = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                                .get("id").asLong();

                UpdateUserRequest request = new UpdateUserRequest();
                request.setRole(UserRole.DEVELOPER);

                mockMvc.perform(put("/api/users/{id}", superAdminId)
                                .header("Authorization", "Bearer " + superAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @Order(14) // 创建新管理员
        void createUser_AsAdmin_CreateAdminRole_ShouldSucceed() throws Exception {
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername("newadmin");
                request.setPassword("password");
                request.setEmail("newadmin@example.com");
                request.setFullName("New Admin");
                request.setRole(UserRole.ADMIN);

                mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + superAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("newadmin"))
                                .andExpect(jsonPath("$.email").value("newadmin@example.com"))
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @Order(15)
        void getUsers_AsNewCreatedAdmin_ShouldReturnAllUsers() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("newadmin");
                loginRequest.setPassword("password");
                MvcResult newUserResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andReturn();
                String newAdminToken = objectMapper.readTree(newUserResult.getResponse().getContentAsString())
                                .get("accessToken").asText();

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + newAdminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.totalElements").isNumber());

                // 管理员ID
                MvcResult adminResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + newAdminToken))
                                .andReturn();
                newAdminId = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                                .get("id").asLong();
        }

        @Test
        @Order(16)
        void updateUserRole_AsAdmin_ShouldSucceed() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setRole(UserRole.ADMIN);

                mockMvc.perform(put("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + superAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @Order(17)
        void testTokenInvalidAfterRoleUpdate() throws Exception {
                // 验证角色更新后原token失效
                mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + newUserToken))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(18)
        void getUsers_AsNewUpdatedAdmin_ShouldReturnAllUsers() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername("newuser");
                loginRequest.setPassword("password");
                MvcResult newUserResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andReturn();
                newUserToken = objectMapper.readTree(newUserResult.getResponse().getContentAsString())
                                .get("accessToken").asText();

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + newUserToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @Order(19)
        void updateUserRole_AsDeveloper_ShouldReturnForbidden() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setRole(UserRole.ADMIN);

                mockMvc.perform(put("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(20)
        void updateSelfInfo_ShouldSucceed() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setEmail("selfupdate@example.com");
                request.setFullName("Self Updated");

                // 获取当前用户ID
                MvcResult userResult = mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + developerToken))
                                .andReturn();
                Long userId = objectMapper.readTree(userResult.getResponse().getContentAsString())
                                .get("id").asLong();

                mockMvc.perform(put("/api/users/{id}", userId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("selfupdate@example.com"))
                                .andExpect(jsonPath("$.fullName").value("Self Updated"));
        }

        @Test
        @Order(21)
        void updateOtherUserInfo_AsDeveloper_ShouldReturnForbidden() throws Exception {
                UpdateUserRequest request = new UpdateUserRequest();
                request.setEmail("hacked@example.com");
                request.setFullName("Hacked User");

                mockMvc.perform(put("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + developerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(22)
        void deleteUser_AsDeveloper_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(delete("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + developerToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(23)
        void deleteUser_AsAdmin_ShouldSucceed() throws Exception {
                mockMvc.perform(delete("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + superAdmin))
                                .andExpect(status().isOk());

                // 验证用户已被删除
                mockMvc.perform(get("/api/users/{id}", newUserId)
                                .header("Authorization", "Bearer " + superAdmin))
                                .andExpect(status().isNotFound());
        }

        @Test
        @Order(24)
        void testTokenInvalidAfterUserDeleted() throws Exception {
                // 验证用户删除后原token失效
                mockMvc.perform(get("/api/users/current")
                                .header("Authorization", "Bearer " + newUserToken))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(24)
        void deleteFinalAdmin_ShouldFailed() throws Exception {
                mockMvc.perform(delete("/api/users/{id}", newAdminId)
                                .header("Authorization", "Bearer " + superAdmin))
                                .andExpect(status().isOk());

                mockMvc.perform(delete("/api/users/{id}", superAdminId)
                                .header("Authorization", "Bearer " + superAdmin))
                                .andExpect(status().isBadRequest());
        }
}
