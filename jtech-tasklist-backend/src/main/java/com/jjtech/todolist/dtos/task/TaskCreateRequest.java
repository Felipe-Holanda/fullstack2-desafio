package com.jjtech.todolist.dtos.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class TaskCreateRequest {
    @NotBlank
    @Size(max = 180)
    private String title;

    private String description;

    private LocalDate dueDate;

    private Long parentTaskId; // optional

    private Set<Long> tagIds; // optional
}
