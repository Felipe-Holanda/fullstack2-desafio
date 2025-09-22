package com.jjtech.todolist.controllers;

import com.jjtech.todolist.dtos.task.TaskCreateRequest;
import com.jjtech.todolist.dtos.task.TaskResponse;
import com.jjtech.todolist.dtos.task.TaskUpdateRequest;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.services.CurrentUserService;
import com.jjtech.todolist.services.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders/{folderId}/tasks")
@Tag(name = "Tarefas")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Criar tarefa ou subtarefa")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long folderId, @Valid @RequestBody TaskCreateRequest req) {
        User me = currentUserService.requireCurrentUser();
        return taskService.create(me, folderId, req);
    }

    @Operation(summary = "Listar tarefas (apenas raízes, com subtarefas)" )
    @GetMapping
    public List<TaskResponse> list(@PathVariable Long folderId) {
        User me = currentUserService.requireCurrentUser();
        return taskService.list(me, folderId);
    }

    @Operation(summary = "Buscar tarefa por id")
    @GetMapping("/{taskId}")
    public TaskResponse getOne(@PathVariable Long folderId, @PathVariable Long taskId) {
        User me = currentUserService.requireCurrentUser();
        return taskService.getById(me, folderId, taskId);
    }

    @Operation(summary = "Atualizar tarefa")
    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long folderId, @PathVariable Long taskId,
                               @Valid @RequestBody TaskUpdateRequest req) {
        User me = currentUserService.requireCurrentUser();
        return taskService.update(me, folderId, taskId, req);
    }

    @Operation(summary = "Marcar tarefa como concluída/não concluída")
    @PatchMapping("/{taskId}/completed")
    public TaskResponse setCompleted(@PathVariable Long folderId, @PathVariable Long taskId,
                                     @RequestParam boolean completed) {
        User me = currentUserService.requireCurrentUser();
        return taskService.toggleCompleted(me, folderId, taskId, completed);
    }

    @Operation(summary = "Excluir tarefa (cascateia subtarefas)")
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long folderId, @PathVariable Long taskId) {
        User me = currentUserService.requireCurrentUser();
        taskService.delete(me, folderId, taskId);
    }
}
