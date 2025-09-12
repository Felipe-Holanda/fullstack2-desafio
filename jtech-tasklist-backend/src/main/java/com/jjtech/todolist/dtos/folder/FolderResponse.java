package com.jjtech.todolist.dtos.folder;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FolderResponse {
    Long id;
    String name;
    @JsonProperty("isPublic")
    boolean isPublic;
    String key;
    Long ownerId;
}
