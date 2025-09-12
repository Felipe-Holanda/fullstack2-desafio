package com.jjtech.todolist.repositories;

import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwner(User owner);
    Optional<Folder> findByKey(String key);
}
