package com.jjtech.todolist.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserResponse {
    @Schema(example = "1")
    Long id;
    @Schema(example = "Maria da Silva")
    String name;
    @Schema(example = "maria@example.com")
    String email;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
