package com.rayfay.gira.repository;

import com.rayfay.gira.entity.Task;
import com.rayfay.gira.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findBySprintId(Long sprintId);

    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId")
    int countBySprintId(Long sprintId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    int countBySprintIdAndStatus(Long sprintId, TaskStatus status);

    List<Task> findBySprintIdAndStatus(Long sprintId, TaskStatus status);

    List<Task> findBySprintIdAndStatusIn(Long sprintId, List<TaskStatus> statuses);
}