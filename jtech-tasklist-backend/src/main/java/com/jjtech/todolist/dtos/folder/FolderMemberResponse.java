package com.jjtech.todolist.dtos.folder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FolderMemberResponse {
    @Schema(description = "ID do usuário membro")
    Long userId;

    @Schema(description = "Nome do usuário")
    String name;

    @Schema(description = "Email do usuário")
    String email;

    @Schema(description = "Data em que entrou na pasta")
    LocalDateTime joinedAt;
}
