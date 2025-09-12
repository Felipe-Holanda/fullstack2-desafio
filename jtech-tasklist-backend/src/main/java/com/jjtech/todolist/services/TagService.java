package com.jjtech.todolist.services;

import com.jjtech.todolist.dtos.tag.TagCreateRequest;
import com.jjtech.todolist.dtos.tag.TagResponse;
import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.Tag;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.repositories.FolderMemberRepository;
import com.jjtech.todolist.repositories.FolderRepository;
import com.jjtech.todolist.repositories.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final FolderRepository folderRepository;
    private final FolderMemberRepository folderMemberRepository;
    private final TagRepository tagRepository;

    @Transactional
    public TagResponse create(User requester, Long folderId, TagCreateRequest req) {
        Folder folder = loadFolderOr404(folderId);
        // only owner can create
        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o dono pode criar tags");
        }
        tagRepository.findByFolderAndNameIgnoreCase(folder, req.getName().trim())
                .ifPresent(t -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag já existe"); });

        Tag tag = Tag.builder()
                .name(req.getName().trim())
                .color(generateLightColor())
                .folder(folder)
                .build();
        tag = tagRepository.save(tag);
        return toResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(User requester, Long folderId) {
        Folder folder = loadFolderOr404(folderId);
        boolean isOwner = folder.getOwner().getId().equals(requester.getId());
        boolean isMember = folderMemberRepository.findByFolderAndUser(folder, requester).isPresent();
        if (!isOwner && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado às tags da pasta");
        }
        return tagRepository.findByFolder(folder).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(User requester, Long folderId, Long tagId) {
        Folder folder = loadFolderOr404(folderId);
        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o dono pode deletar tags");
        }
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag não encontrada"));
        if (!tag.getFolder().getId().equals(folderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag não pertence à pasta");
        }
        // Detach from tasks via JPA relation table by clearing associations from owning side (Task.tags)
        // Since Task is the owning side of @ManyToMany, we need to iterate tasks in the folder and remove the tag.
        folder.getTasks().forEach(t -> t.getTags().removeIf(existing -> existing.getId().equals(tag.getId())));
        tagRepository.delete(tag);
    }

    private Folder loadFolderOr404(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));
    }

    private String generateLightColor() {
        SecureRandom rnd = new SecureRandom();
        int r = 180 + rnd.nextInt(76); // 180-255
        int g = 180 + rnd.nextInt(76);
        int b = 180 + rnd.nextInt(76);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private TagResponse toResponse(Tag t) {
        return TagResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .color(t.getColor())
                .folderId(t.getFolder().getId())
                .build();
    }
}
