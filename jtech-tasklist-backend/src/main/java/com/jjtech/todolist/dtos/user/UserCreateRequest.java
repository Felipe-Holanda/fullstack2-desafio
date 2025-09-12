package com.jjtech.todolist.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UserCreateRequest {

    @Schema(example = "Maria da Silva")
    @NotBlank
    private String name;

    @Schema(example = "maria@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "StrongPassword123")
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
