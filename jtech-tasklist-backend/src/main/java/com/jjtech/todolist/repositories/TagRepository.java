package com.jjtech.todolist.repositories;

import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByFolder(Folder folder);
    Optional<Tag> findByFolderAndNameIgnoreCase(Folder folder, String name);
}
