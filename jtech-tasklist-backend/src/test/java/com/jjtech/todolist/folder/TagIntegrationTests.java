package com.jjtech.todolist.folder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjtech.todolist.dtos.auth.LoginRequest;
import com.jjtech.todolist.dtos.folder.FolderCreateRequest;
import com.jjtech.todolist.dtos.tag.TagCreateRequest;
import com.jjtech.todolist.dtos.user.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TagIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private record AuthCtx(long userId, String token) {}

    private AuthCtx registerAndLogin(String name, String email) throws Exception {
        // register
        UserCreateRequest reg = new UserCreateRequest();
        reg.setName(name);
        reg.setEmail(email);
        reg.setPassword("password123");
        String regResp = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long uid = objectMapper.readTree(regResp).get("id").asLong();

        // login
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword("password123");
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResp).get("token").asText();
        return new AuthCtx(uid, token);
    }

    @Test
    void tag_create_list_delete_and_permissions() throws Exception {
        AuthCtx owner = registerAndLogin("Carol", "carol@example.com");
        AuthCtx bob = registerAndLogin("Bob2", "bob2@example.com");

        // owner creates a private folder
        FolderCreateRequest create = new FolderCreateRequest();
        create.setName("Com Tags");
        create.setPublic(false);
        String folderResp = mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long folderId = objectMapper.readTree(folderResp).get("id").asLong();

        // non-member cannot list tags (403)
        mockMvc.perform(get("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isForbidden());

        // owner creates 2 tags
        TagCreateRequest t1 = new TagCreateRequest(); t1.setName("Urgente");
        String tag1 = mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.color").isString())
                .andReturn().getResponse().getContentAsString();
        long tagId = objectMapper.readTree(tag1).get("id").asLong();

        TagCreateRequest t2 = new TagCreateRequest(); t2.setName("Estudos");
        mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t2)))
                .andExpect(status().isCreated());

        // duplicate name -> 409
        mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t2)))
                .andExpect(status().isConflict());

        // owner lists -> sees >= 2
        String listOwner = mockMvc.perform(get("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String,Object>> tags = objectMapper.readValue(listOwner, new TypeReference<>(){});
        assertThat(tags).hasSizeGreaterThanOrEqualTo(2);

        // owner removes Bob as non-member check: still cannot create
        TagCreateRequest t3 = new TagCreateRequest(); t3.setName("Pessoal");
        mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t3)))
                .andExpect(status().isForbidden());

        // delete by owner works
        mockMvc.perform(delete("/api/folders/" + folderId + "/tags/" + tagId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());
    }
}
