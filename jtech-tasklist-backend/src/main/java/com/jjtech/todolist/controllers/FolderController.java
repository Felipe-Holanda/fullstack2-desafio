package com.jjtech.todolist.controllers;

import com.jjtech.todolist.dtos.folder.FolderCreateRequest;
import com.jjtech.todolist.dtos.folder.FolderJoinRequest;
import com.jjtech.todolist.dtos.folder.FolderResponse;
import com.jjtech.todolist.dtos.folder.FolderMemberResponse;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.services.CurrentUserService;
import com.jjtech.todolist.services.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "Pastas")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Criar pasta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody FolderCreateRequest req) {
        User me = currentUserService.requireCurrentUser();
        return folderService.createFolder(me, req);
    }

    @Operation(summary = "Listar minhas pastas")
    @GetMapping
    public List<FolderResponse> listMine() {
        User me = currentUserService.requireCurrentUser();
        return folderService.listMine(me);
    }

    @Operation(summary = "Listar pastas que participo (não sou dono)")
    @GetMapping("/participating")
    public List<FolderResponse> listParticipating() {
        User me = currentUserService.requireCurrentUser();
        return folderService.listParticipating(me);
    }

    @Operation(summary = "Listar todas as pastas (sou dono ou participo)")
    @GetMapping("/all")
    public List<FolderResponse> listAllMineAndParticipating() {
        User me = currentUserService.requireCurrentUser();
        return folderService.listOwnedAndParticipating(me);
    }

    @Operation(summary = "Entrar em pasta pública via chave")
    @PostMapping("/join")
    public FolderResponse join(@Valid @RequestBody FolderJoinRequest req) {
        User me = currentUserService.requireCurrentUser();
        return folderService.joinPublicByKey(me, req.getKey());
    }

    @Operation(summary = "Excluir pasta (somente dono)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User me = currentUserService.requireCurrentUser();
        folderService.deleteFolder(me, id);
    }

    @Operation(summary = "Rotacionar chave (somente dono, pasta pública)")
    @PostMapping("/{id}/rotate-key")
    public FolderResponse rotate(@PathVariable Long id) {
        User me = currentUserService.requireCurrentUser();
        return folderService.rotateKey(me, id);
    }

    @Operation(summary = "Remover membro (somente dono)")
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        User me = currentUserService.requireCurrentUser();
        folderService.removeMember(me, id, userId);
    }

    @Operation(summary = "Listar membros da pasta (dono ou membro)")
    @GetMapping("/{id}/members")
    public List<FolderMemberResponse> listMembers(@PathVariable Long id) {
        User me = currentUserService.requireCurrentUser();
        return folderService.listMembers(me, id);
    }
}
