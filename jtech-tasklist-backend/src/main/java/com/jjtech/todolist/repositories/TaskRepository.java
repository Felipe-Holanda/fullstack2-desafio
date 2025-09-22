package com.jjtech.todolist.repositories;

import com.jjtech.todolist.entities.Folder;
import com.jjtech.todolist.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByFolder(Folder folder);
    long countByParentTask(Task parent);
    Optional<Task> findByIdAndFolder(Long id, Folder folder);
}
