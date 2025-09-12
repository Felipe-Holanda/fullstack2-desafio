package com.jjtech.todolist.services;

import com.jjtech.todolist.dtos.user.UserCreateRequest;
import com.jjtech.todolist.dtos.user.UserResponse;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserCreateRequest req) {
        userRepository.findByEmailIgnoreCase(req.getEmail())
                .ifPresent(u -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado"); });

    User user = User.builder()
                .name(req.getName())
                .email(req.getEmail().trim().toLowerCase())
        .password(passwordEncoder.encode(req.getPassword()))
                .build();
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Violação de integridade (email único)");
        }

        return toResponse(user);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
