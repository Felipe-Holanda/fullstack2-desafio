package com.jjtech.todolist.folder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjtech.todolist.dtos.auth.LoginRequest;
import com.jjtech.todolist.dtos.folder.FolderCreateRequest;
import com.jjtech.todolist.dtos.folder.FolderJoinRequest;
import com.jjtech.todolist.dtos.user.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FolderIntegrationTests {

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
    void create_list_join_rotate_remove_delete_flow() throws Exception {
        AuthCtx owner = registerAndLogin("Alice", "alice@example.com");
        AuthCtx bob = registerAndLogin("Bob", "bob@example.com");

        // create private folder
        FolderCreateRequest createPrivate = new FolderCreateRequest();
        createPrivate.setName("Privada");
        createPrivate.setPublic(false);
        String privResp = mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPrivate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.isPublic").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode priv = objectMapper.readTree(privResp);
        long privateId = priv.get("id").asLong();

        // create public folder
        FolderCreateRequest createPublic = new FolderCreateRequest();
        createPublic.setName("Publica");
        createPublic.setPublic(true);
        String pubResp = mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPublic)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.key").isString())
                .andReturn().getResponse().getContentAsString();
        JsonNode pub = objectMapper.readTree(pubResp);
        long publicId = pub.get("id").asLong();
        String key = pub.get("key").asText();
        assertThat(key).hasSize(8);

        // members list (owner sees at least himself)
        String membersOwner = mockMvc.perform(get("/api/folders/" + publicId + "/members")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arrMembersOwner = objectMapper.readTree(membersOwner);
        assertThat(arrMembersOwner.isArray()).isTrue();
        assertThat(arrMembersOwner.size()).isGreaterThanOrEqualTo(1);

        // list mine returns at least 2
        String listResp = mockMvc.perform(get("/api/folders")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(listResp);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThanOrEqualTo(2);

        // bob joins public by key
        FolderJoinRequest joinReq = new FolderJoinRequest();
        joinReq.setKey(key);
        mockMvc.perform(post("/api/folders/join")
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId));

        // bob can list members and should see at least 2 now
        String membersBob = mockMvc.perform(get("/api/folders/" + publicId + "/members")
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(membersBob).size()).isGreaterThanOrEqualTo(2);

        // owner rotates key
        String rotatedResp = mockMvc.perform(post("/api/folders/" + publicId + "/rotate-key")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newKey = objectMapper.readTree(rotatedResp).get("key").asText();
        assertThat(newKey).hasSize(8).isNotEqualTo(key);

        // owner removes bob from folder
        mockMvc.perform(delete("/api/folders/" + publicId + "/members/" + bob.userId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());

        // bob cannot list members anymore (403)
        mockMvc.perform(get("/api/folders/" + publicId + "/members")
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isForbidden());

        // non-owner trying to delete private folder -> 403
        mockMvc.perform(delete("/api/folders/" + privateId)
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isForbidden());

        // owner deletes both folders
        mockMvc.perform(delete("/api/folders/" + publicId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/folders/" + privateId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());
    }
}
