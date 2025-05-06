package com.rayfay.gira.dto.request;

import com.rayfay.gira.entity.TaskPriority;
import lombok.Data;

@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private Long assigneeId;
}