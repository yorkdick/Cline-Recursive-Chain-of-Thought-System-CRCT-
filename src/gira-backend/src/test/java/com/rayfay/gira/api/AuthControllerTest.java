package com.rayfay.gira.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.request.RefreshTokenRequest;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private String refreshToken;

    @Test
    @Order(1)
    void login_WithValidCredentials_ShouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("manager");
        request.setPassword("password");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists())
                .andReturn();

        // 保存token供后续测试使用
        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
        refreshToken = objectMapper.readTree(response).get("refreshToken").asText();
    }

    @Test
    @Order(2)
    void login_WithInvalidPassword_ShouldReturnUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("manager");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @Order(3)
    void login_WithNonexistentUser_ShouldReturnUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @Order(4)
    void refreshToken_WithValidToken_ShouldReturnNewAccessToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @Order(5)
    void refreshToken_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.refresh.token");

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无效的刷新令牌"));
    }

    @Test
    @Order(6)
    void logout_WithValidToken_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 验证使用已登出的token访问受保护的接口应该返回未授权
        mockMvc.perform(get("/api/users/current")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void logout_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    void logout_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }
}