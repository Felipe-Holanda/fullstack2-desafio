package com.jjtech.todolist.dtos.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FolderJoinRequest {
    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "Chave deve conter exatamente 8 dígitos")
    @Schema(example = "12345678")
    private String key;
}
