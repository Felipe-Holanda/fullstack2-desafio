package com.jjtech.todolist.folder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjtech.todolist.dtos.auth.LoginRequest;
import com.jjtech.todolist.dtos.folder.FolderCreateRequest;
import com.jjtech.todolist.dtos.tag.TagCreateRequest;
import com.jjtech.todolist.dtos.task.TaskCreateRequest;
import com.jjtech.todolist.dtos.task.TaskUpdateRequest;
import com.jjtech.todolist.dtos.user.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskIntegrationTests {

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
    void tasks_crud_permissions_and_subtasks() throws Exception {
        AuthCtx owner = registerAndLogin("Dora", "dora@example.com");
        AuthCtx bob = registerAndLogin("BobT", "bobt@example.com");

        // create private folder
        FolderCreateRequest create = new FolderCreateRequest();
        create.setName("Com Tarefas");
        create.setPublic(false);
        String folderResp = mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long folderId = objectMapper.readTree(folderResp).get("id").asLong();

        // Owner creates two tags
        TagCreateRequest t1 = new TagCreateRequest(); t1.setName("Urgente");
        String tag1 = mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tagId1 = objectMapper.readTree(tag1).get("id").asLong();

        TagCreateRequest t2 = new TagCreateRequest(); t2.setName("Casa");
        String tag2 = mockMvc.perform(post("/api/folders/" + folderId + "/tags")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tagId2 = objectMapper.readTree(tag2).get("id").asLong();

        // Non-member cannot list tasks
        mockMvc.perform(get("/api/folders/" + folderId + "/tasks")
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isForbidden());

        // Owner creates a root task with tags
        TaskCreateRequest tc = new TaskCreateRequest();
        tc.setTitle("Comprar mantimentos");
        tc.setDescription("Lista da semana");
        tc.setDueDate(LocalDate.now().plusDays(2));
        tc.setTagIds(Set.of(tagId1, tagId2));
        String taskResp = mockMvc.perform(post("/api/folders/" + folderId + "/tasks")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tc)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tagIds").isArray())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskResp).get("id").asLong();

        // Create 5 subtasks -> ok, 6th should fail
        for (int i = 1; i <= 5; i++) {
            TaskCreateRequest st = new TaskCreateRequest();
            st.setTitle("Item " + i);
            st.setParentTaskId(taskId);
            mockMvc.perform(post("/api/folders/" + folderId + "/tasks")
                            .header("Authorization", "Bearer " + owner.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(st)))
                    .andExpect(status().isCreated());
        }
        TaskCreateRequest st6 = new TaskCreateRequest(); st6.setTitle("Item 6"); st6.setParentTaskId(taskId);
        mockMvc.perform(post("/api/folders/" + folderId + "/tasks")
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(st6)))
                .andExpect(status().isBadRequest());

        // List returns the root with 5 subtasks
        String listOwner = mockMvc.perform(get("/api/folders/" + folderId + "/tasks")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String,Object>> tasks = objectMapper.readValue(listOwner, new TypeReference<>(){});
        assertThat(tasks).hasSize(1);
        assertThat((List<?>) tasks.get(0).get("subtasks")).hasSize(5);

        // Update title and completion
        TaskUpdateRequest up = new TaskUpdateRequest();
        up.setTitle("Comprar mantimentos atualizada");
        up.setCompleted(true);
        String upd = mockMvc.perform(put("/api/folders/" + folderId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + owner.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(up)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(upd).get("title").asText()).contains("atualizada");

        // Toggle completed back false via PATCH
        mockMvc.perform(patch("/api/folders/" + folderId + "/tasks/" + taskId + "/completed?completed=false")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        // Delete task cascades subtasks
        mockMvc.perform(delete("/api/folders/" + folderId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isNoContent());

        // now list is empty
        String listAfter = mockMvc.perform(get("/api/folders/" + folderId + "/tasks")
                        .header("Authorization", "Bearer " + owner.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(listAfter).isEmpty()).isTrue();
    }
}
