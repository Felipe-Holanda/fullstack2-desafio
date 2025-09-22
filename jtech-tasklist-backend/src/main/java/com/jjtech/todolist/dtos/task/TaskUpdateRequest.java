package com.jjtech.todolist.dtos.task;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class TaskUpdateRequest {
    @Size(max = 180)
    private String title;

    private String description;

    private LocalDate dueDate;

    private Boolean completed;

    private Set<Long> tagIds;
}
