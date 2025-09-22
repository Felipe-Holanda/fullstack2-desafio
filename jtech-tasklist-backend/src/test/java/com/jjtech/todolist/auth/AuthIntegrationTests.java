package com.jjtech.todolist.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjtech.todolist.dtos.auth.LoginRequest;
import com.jjtech.todolist.dtos.user.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void login_and_access_secured_endpoint() throws Exception {
	// register user
	UserCreateRequest req = new UserCreateRequest();
	req.setName("Joao");
	req.setEmail("joao@example.com");
	req.setPassword("password123");
	String regBody = objectMapper.writeValueAsString(req);

	String regResp = mockMvc.perform(post("/api/users/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content(regBody))
		.andExpect(status().isCreated())
		.andReturn().getResponse().getContentAsString();
	JsonNode regJson = objectMapper.readTree(regResp);
	long userId = regJson.get("id").asLong();

	// login
	LoginRequest login = new LoginRequest();
	login.setEmail("joao@example.com");
	login.setPassword("password123");
	String loginResp = mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(login)))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.token").exists())
		.andReturn().getResponse().getContentAsString();
	String token = objectMapper.readTree(loginResp).get("token").asText();

	// access without token -> 401
	mockMvc.perform(get("/api/users/" + userId))
		.andExpect(status().isUnauthorized());

	// access with token -> 200
	mockMvc.perform(get("/api/users/" + userId)
			.header("Authorization", "Bearer " + token))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.email").value("joao@example.com"));
    }
}
