package com.rayfay.gira.service.impl;

import com.rayfay.gira.dto.request.CreateTaskRequest;
import com.rayfay.gira.dto.request.UpdateTaskRequest;
import com.rayfay.gira.dto.request.UpdateTaskStatusRequest;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.*;
import com.rayfay.gira.exception.ResourceNotFoundException;
import com.rayfay.gira.mapper.TaskMapper;
import com.rayfay.gira.repository.SprintRepository;
import com.rayfay.gira.repository.TaskRepository;
import com.rayfay.gira.repository.UserRepository;
import com.rayfay.gira.service.interfaces.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (request.getSprintId() == null) {
            throw new IllegalArgumentException("任务必须在Sprint中创建");
        }

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));
        if (sprint.getStatus() != SprintStatus.PLANNING && sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalStateException("只能将任务添加到计划中或活动中的Sprint");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reporter(currentUser)
                .sprint(sprint)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("指派人不存在"));
            task.setAssignee(assignee);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskOrThrow(id);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("指派人不存在"));
            task.setAssignee(assignee);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request) {
        Task task = getTaskOrThrow(id);

        // 检查状态转换是否有效
        if (!isValidStatusTransition(task.getStatus(), request.getStatus())) {
            throw new IllegalStateException("无效的状态变更");
        }

        task.setStatus(request.getStatus());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    private boolean isValidStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }

        switch (currentStatus) {
            case TODO:
                return newStatus == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS:
                return newStatus == TaskStatus.DONE || newStatus == TaskStatus.TODO;
            case DONE:
                return false;
            default:
                return false;
        }
    }

    @Override
    public TaskResponse getTaskById(Long id) {
        return taskMapper.toResponse(getTaskOrThrow(id));
    }

    @Override
    @Transactional
    public TaskResponse moveTaskToSprint(Long id, Long sprintId) {
        Task task = getTaskOrThrow(id);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));

        if (sprint.getStatus() != SprintStatus.PLANNING && sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalStateException("只能将任务移动到计划中或活动中的Sprint");
        }

        task.setSprint(sprint);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public Page<TaskResponse> getTasksByAssignee(Long assigneeId, Pageable pageable) {
        return taskRepository.findByAssigneeId(assigneeId, pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
    }
}