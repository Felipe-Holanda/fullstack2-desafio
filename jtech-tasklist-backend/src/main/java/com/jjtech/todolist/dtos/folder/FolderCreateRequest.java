package com.jjtech.todolist.dtos.folder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderCreateRequest {
    @NotBlank
    @Schema(example = "Projetos")
    private String name;

    @Schema(description = "Se a pasta é pública", example = "true")
    private boolean isPublic;
}
