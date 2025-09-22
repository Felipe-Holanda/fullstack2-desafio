package com.jjtech.todolist.services;

import com.jjtech.todolist.dtos.folder.FolderCreateRequest;
import com.jjtech.todolist.dtos.folder.FolderResponse;
import com.jjtech.todolist.dtos.folder.FolderMemberResponse;
import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.FolderMember;
import com.jjtech.todolist.entities.User;
import com.jjtech.todolist.repositories.FolderMemberRepository;
import com.jjtech.todolist.repositories.FolderRepository;
import lombok.RequiredArgsConstructor;
import java.security.SecureRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final FolderMemberRepository folderMemberRepository;

    @Transactional
    public FolderResponse createFolder(User owner, FolderCreateRequest req) {
        Folder folder = Folder.builder()
                .name(req.getName())
                .owner(owner)
                .isPublic(req.isPublic())
                .key(req.isPublic() ? generateKey() : null)
                .build();
        folder = folderRepository.save(folder);

        FolderMember fm = FolderMember.builder().folder(folder).user(owner).build();
        folderMemberRepository.save(fm);

        return toResponse(folder);
    }

    @Transactional
    public void deleteFolder(User requester, Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));
        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador pode deletar");
        }
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderResponse rotateKey(User requester, Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));
        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador pode alterar a chave");
        }
        if (!folder.isPublic()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas pastas públicas possuem chave");
        }
        folder.setKey(generateKey());
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderResponse joinPublicByKey(User user, String key) {
        Folder folder = folderRepository.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chave inválida"));
        if (!folder.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pasta não é pública");
        }
        folderMemberRepository.findByFolderAndUser(folder, user)
                .ifPresent(fm -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Já é membro"); });
        FolderMember fm = FolderMember.builder().folder(folder).user(user).build();
        folderMemberRepository.save(fm);
        return toResponse(folder);
    }

    @Transactional
    public void removeMember(User requester, Long folderId, Long userIdToRemove) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));
        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o criador pode remover membros");
        }
        User dummy = new User(); dummy.setId(userIdToRemove);
        var fmOpt = folderMemberRepository.findByFolderAndUser(folder, dummy);
        if (fmOpt.isEmpty()) return; // nada a remover
        folderMemberRepository.delete(fmOpt.get());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listMine(User user) {
        return folderRepository.findByOwner(user)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listParticipating(User user) {
        return folderMemberRepository.findByUser(user).stream()
                .map(FolderMember::getFolder)
                .filter(f -> f != null)
                .filter(f -> !f.getOwner().getId().equals(user.getId())) // only participating, not owned
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listOwnedAndParticipating(User user) {
        var owned = folderRepository.findByOwner(user);
        var participating = folderMemberRepository.findByUser(user).stream()
                .map(FolderMember::getFolder)
                .filter(f -> f != null)
                .collect(Collectors.toList());

        var map = new java.util.LinkedHashMap<Long, Folder>();
        for (Folder f : owned) map.put(f.getId(), f);
        for (Folder f : participating) map.putIfAbsent(f.getId(), f);

        return map.values().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderMemberResponse> listMembers(User requester, Long folderId) {
    Folder folder = folderRepository.findById(folderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta não encontrada"));

    boolean isOwner = folder.getOwner().getId().equals(requester.getId());
    boolean isMember = folderMemberRepository.findByFolderAndUser(folder, requester).isPresent();
    if (!isOwner && !isMember) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a membros da pasta");
    }

    return folder.getMembers().stream()
        .map(m -> FolderMemberResponse.builder()
            .userId(m.getUser().getId())
            .name(m.getUser().getName())
            .email(m.getUser().getEmail())
            .joinedAt(m.getCreatedAt())
            .build())
        .collect(Collectors.toList());
    }

    public String generateKey() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }

    private FolderResponse toResponse(Folder f) {
        return FolderResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .isPublic(f.isPublic())
                .key(f.getKey())
                .ownerId(f.getOwner().getId())
                .build();
    }
}
