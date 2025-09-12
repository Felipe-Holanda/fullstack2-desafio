package com.jjtech.todolist.dtos.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubtaskResponse {
    Long id;
    String title;
    boolean completed;
}
