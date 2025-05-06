package com.rayfay.gira.service.interfaces;

import com.rayfay.gira.dto.request.CreateTaskRequest;
import com.rayfay.gira.dto.request.UpdateTaskRequest;
import com.rayfay.gira.dto.request.UpdateTaskStatusRequest;
import com.rayfay.gira.dto.response.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request);

    TaskResponse getTaskById(Long id);

    TaskResponse moveTaskToSprint(Long id, Long sprintId);

    Page<TaskResponse> getTasksByAssignee(Long assigneeId, Pageable pageable);

    void deleteTask(Long id);
}