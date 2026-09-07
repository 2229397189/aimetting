package com.aimeeting.interview.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimeeting.interview.auth.api.io.req.LoginReq;
import com.aimeeting.interview.auth.api.io.req.RegisterReq;
import com.aimeeting.interview.common.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 用户与认证接口端到端测试（MockMvc + H2）。
 *
 * <p>断言重点：不只看 HTTP 状态，而是校验返回体 {@code code}、token 合法性、
 * 以及受保护接口在缺失令牌时的错误码。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("注册成功：code=0，data 含 userId/username 与合法三段 JWT")
    void registerSuccess() throws Exception {
        String username = "u" + UUID.randomUUID().toString().substring(0, 8);
        RegisterReq req = new RegisterReq();
        req.setUsername(username);
        req.setPassword("test12345");
        req.setEmail(username + "@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.token.accessToken").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andReturn();

        JsonNode data = JsonUtil.MAPPER.readTree(result.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("token").get("accessToken").asText();
        assertEquals(3, accessToken.split("\\.").length, "accessToken 应为合法三段 JWT");
        assertTrue(data.get("userId").asLong() > 0L, "userId 应为正数");
    }

    @Test
    @DisplayName("注册重复用户名：code=A0104")
    void registerDuplicateUsername() throws Exception {
        String username = "dup" + UUID.randomUUID().toString().substring(0, 8);
        RegisterReq req = new RegisterReq();
        req.setUsername(username);
        req.setPassword("test12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0104"));
    }

    @Test
    @DisplayName("注册参数非法（密码 3 位）：code 以 A01 开头且 HTTP 400")
    void registerInvalidParam() throws Exception {
        String body = "{\"username\":\"ab1\",\"password\":\"123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0103"));
    }

    @Test
    @DisplayName("登录成功：code=0，返回 accessToken 与 userId 一致的用户资料")
    void loginSuccessThenAccessProfile() throws Exception {
        String username = "lg" + UUID.randomUUID().toString().substring(0, 8);
        String password = "test12345";

        RegisterReq registerReq = new RegisterReq();
        registerReq.setUsername(username);
        registerReq.setPassword(password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        LoginReq loginReq = new LoginReq();
        loginReq.setUsername(username);
        loginReq.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.token.accessToken").isString())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andReturn();

        JsonNode loginData = JsonUtil.MAPPER
                .readTree(loginResult.getResponse().getContentAsString()).get("data");
        String accessToken = loginData.get("token").get("accessToken").asText();
        long userId = loginData.get("user").get("userId").asLong();

        // 携带 token 访问受保护接口
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    @DisplayName("错误密码登录：code=A0101")
    void loginWithWrongPassword() throws Exception {
        String username = "wp" + UUID.randomUUID().toString().substring(0, 8);
        RegisterReq registerReq = new RegisterReq();
        registerReq.setUsername(username);
        registerReq.setPassword("test12345");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(registerReq)))
                .andExpect(status().isOk());

        LoginReq loginReq = new LoginReq();
        loginReq.setUsername(username);
        loginReq.setPassword("wrongpass1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(loginReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A0101"));
    }

    @Test
    @DisplayName("无 token 访问受保护接口：HTTP 401 且 code=A0201")
    void accessProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    @DisplayName("内置账号 admin 可登录（DataInitializer 生效）")
    void builtinAdminCanLogin() throws Exception {
        LoginReq loginReq = new LoginReq();
        loginReq.setUsername("admin");
        loginReq.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("健康检查：code=0 且 data.status=UP、db=UP")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.db").value("UP"))
                .andExpect(jsonPath("$.data.aiProvider").value("deepseek"));
    }

    @Test
    @DisplayName("退出登录后旧 token 访问受保护接口返回 401")
    void logoutThenOldTokenRejected() throws Exception {
        String username = "lo" + UUID.randomUUID().toString().substring(0, 8);
        RegisterReq registerReq = new RegisterReq();
        registerReq.setUsername(username);
        registerReq.setPassword("test12345");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(registerReq)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonUtil.MAPPER
                .readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("token").get("accessToken").asText();

        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    @DisplayName("两次加密后的密码摘要不同，证明 BCrypt 加盐生效")
    void passwordHashIsSalted() {
        String first = com.aimeeting.interview.auth.domain.PasswordPolicy.encode("test12345");
        String second = com.aimeeting.interview.auth.domain.PasswordPolicy.encode("test12345");
        assertNotEquals(first, second);
        assertTrue(com.aimeeting.interview.auth.domain.PasswordPolicy.matches("test12345", first));
    }
}
