package com.jjtech.todolist.dtos.tag;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TagResponse {
    Long id;
    String name;
    String color;
    Long folderId;
}
