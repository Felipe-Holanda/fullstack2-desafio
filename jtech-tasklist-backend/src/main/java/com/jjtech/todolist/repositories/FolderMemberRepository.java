package com.jjtech.todolist.repositories;

import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.FolderMember;
import com.jjtech.todolist.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderMemberRepository extends JpaRepository<FolderMember, Long> {
    List<FolderMember> findByUser(User user);
    Optional<FolderMember> findByFolderAndUser(Folder folder, User user);
    Optional<FolderMember> findByFolderIdAndUserId(Long folderId, Long userId);
}
