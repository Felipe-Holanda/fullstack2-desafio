package com.jjtech.todolist.services;

import com.jjtech.todolist.dtos.task.SubtaskResponse;
import com.jjtech.todolist.dtos.task.TaskCreateRequest;
import com.jjtech.todolist.dtos.task.TaskResponse;
import com.jjtech.todolist.dtos.task.TaskUpdateRequest;
import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.Tag;
import com.jjtech.todolist.entities.Task;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.repositories.FolderMemberRepository;
import com.jjtech.todolist.repositories.FolderRepository;
import com.jjtech.todolist.repositories.TagRepository;
import com.jjtech.todolist.repositories.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    private static final int MAX_SUBTASKS = 5;

    private final FolderRepository folderRepository;
    private final FolderMemberRepository folderMemberRepository;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(User requester, Long folderId, TaskCreateRequest req) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);

        Task parent = null;
        if (req.getParentTaskId() != null) {
            parent = taskRepository.findById(req.getParentTaskId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa pai não encontrada"));
            if (!parent.getFolder().getId().equals(folderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtarefa deve estar na mesma pasta da tarefa pai");
            }
            long count = taskRepository.countByParentTask(parent);
            if (count >= MAX_SUBTASKS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite de 5 subtarefas excedido");
            }
        }

        Set<Tag> tags = resolveTags(folder, req.getTagIds());

        Task task = Task.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .dueDate(req.getDueDate())
                .completed(false)
                .folder(folder)
                .parentTask(parent)
                .build();
        task.setTags(tags);
        task = taskRepository.save(task);
        return toResponse(task, true);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(User requester, Long folderId) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);
        List<Task> tasks = taskRepository.findByFolder(folder);
        // Return only root tasks with nested subtasks
        return tasks.stream()
                .filter(t -> t.getParentTask() == null)
                .map(t -> toResponse(t, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(User requester, Long folderId, Long taskId) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);
        Task task = taskRepository.findByIdAndFolder(taskId, folder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
        return toResponse(task, true);
    }

    @Transactional
    public TaskResponse update(User requester, Long folderId, Long taskId, TaskUpdateRequest req) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);
        Task task = taskRepository.findByIdAndFolder(taskId, folder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        if (req.getTitle() != null) task.setTitle(req.getTitle().trim());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getDueDate() != null) task.setDueDate(req.getDueDate());
        if (req.getCompleted() != null) task.setCompleted(req.getCompleted());

        if (req.getTagIds() != null) {
            Set<Tag> tags = resolveTags(folder, req.getTagIds());
            task.getTags().clear();
            task.getTags().addAll(tags);
        }

        task = taskRepository.save(task);
        return toResponse(task, true);
    }

    @Transactional
    public TaskResponse toggleCompleted(User requester, Long folderId, Long taskId, boolean completed) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);
        Task task = taskRepository.findByIdAndFolder(taskId, folder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
        task.setCompleted(completed);
        task = taskRepository.save(task);
        return toResponse(task, false);
    }

    @Transactional
    public void delete(User requester, Long folderId, Long taskId) {
        Folder folder = loadFolderOr404(folderId);
        ensureMemberOrOwner(requester, folder);
        Task task = taskRepository.findByIdAndFolder(taskId, folder)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
        taskRepository.delete(task);
    }

    private Folder loadFolderOr404(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));
    }

    private void ensureMemberOrOwner(User requester, Folder folder) {
        boolean isOwner = folder.getOwner().getId().equals(requester.getId());
        boolean isMember = folderMemberRepository.findByFolderAndUser(folder, requester).isPresent();
        if (!isOwner && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado às tarefas da pasta");
        }
    }

    private Set<Tag> resolveTags(Folder folder, Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        Set<Tag> tags = new HashSet<>();
        for (Long tid : tagIds) {
            Tag tag = tagRepository.findById(tid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag inválida: " + tid));
            if (!tag.getFolder().getId().equals(folder.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag não pertence à pasta");
            }
            tags.add(tag);
        }
        return tags;
    }

    private TaskResponse toResponse(Task t, boolean includeSubtasks) {
        List<SubtaskResponse> subs = List.of();
        if (includeSubtasks) {
            subs = t.getSubtasks().stream()
                    .map(s -> SubtaskResponse.builder()
                            .id(s.getId())
                            .title(s.getTitle())
                            .completed(s.isCompleted())
                            .build())
                    .collect(Collectors.toList());
        }

        Set<Long> tagIds = t.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .dueDate(t.getDueDate())
                .completed(t.isCompleted())
                .folderId(t.getFolder().getId())
                .parentTaskId(t.getParentTask() != null ? t.getParentTask().getId() : null)
                .tagIds(tagIds)
                .subtasks(subs)
                .build();
    }
}
