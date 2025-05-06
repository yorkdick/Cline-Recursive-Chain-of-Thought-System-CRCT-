package com.rayfay.gira.dto.request;

import com.rayfay.gira.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    private TaskPriority priority;

    private Long sprintId;

    private Long assigneeId;
}