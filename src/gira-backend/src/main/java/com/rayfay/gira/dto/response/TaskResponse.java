package com.rayfay.gira.dto.response;

import com.rayfay.gira.entity.TaskPriority;
import com.rayfay.gira.entity.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long sprintId;
    private String sprintName;
    private UserResponse assignee;
    private UserResponse reporter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}