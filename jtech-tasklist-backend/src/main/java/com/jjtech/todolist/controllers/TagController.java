package com.jjtech.todolist.controllers;

import com.jjtech.todolist.dtos.tag.TagCreateRequest;
import com.jjtech.todolist.dtos.tag.TagResponse;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.services.CurrentUserService;
import com.jjtech.todolist.services.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders/{folderId}/tags")
@Tag(name = "Tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Criar tag (somente dono da pasta)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse create(@PathVariable Long folderId, @Valid @RequestBody TagCreateRequest req) {
        User me = currentUserService.requireCurrentUser();
        return tagService.create(me, folderId, req);
    }

    @Operation(summary = "Listar tags (dono ou membro)")
    @GetMapping
    public List<TagResponse> list(@PathVariable Long folderId) {
        User me = currentUserService.requireCurrentUser();
        return tagService.list(me, folderId);
    }

    @Operation(summary = "Excluir tag (somente dono)")
    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long folderId, @PathVariable Long tagId) {
        User me = currentUserService.requireCurrentUser();
        tagService.delete(me, folderId, tagId);
    }
}
