package com.jjtech.todolist.dtos.task;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class TaskResponse {
    Long id;
    String title;
    String description;
    LocalDate dueDate;
    boolean completed;
    Long folderId;
    Long parentTaskId;
    Set<Long> tagIds;
    List<SubtaskResponse> subtasks;
}
